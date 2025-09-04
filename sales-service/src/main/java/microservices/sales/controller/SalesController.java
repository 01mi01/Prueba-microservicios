package microservices.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import microservices.sales.entity.Sale;
import microservices.sales.repository.SaleRepository;
import microservices.sales.client.WarehouseClient;
import microservices.sales.client.ProductResponse;
import microservices.sales.client.ProductUpdateRequest;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private WarehouseClient warehouseClient;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping
    public ResponseEntity<Sale> createSale(@RequestParam Integer productId, @RequestParam Integer quantity,
            @RequestParam(required = false) BigDecimal testPrice) {
        Integer originalStock = null;
        try {
            // 1. Get product from warehouse
            ProductResponse product = warehouseClient.getProduct(productId);
            if (product.getStockQuantity() < quantity) {
                return ResponseEntity.badRequest().build();
            }
            originalStock = product.getStockQuantity();

            // 2. Update stock (reduce)
            int newStock = product.getStockQuantity() - quantity;
            warehouseClient.updateStock(productId, new ProductUpdateRequest(newStock));

            // 3. Create sale
            String saleNumber = "SALE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            BigDecimal unitPrice = testPrice != null ? testPrice : product.getPrice();
            Sale sale = new Sale(saleNumber, productId, quantity, unitPrice);
            sale.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            Sale saved = saleRepository.save(sale);

            // 4. Create accounting entries (THIS WILL FAIL IF testPrice = 0.99)
            createAccountingEntry("1200", "Accounts Receivable", "Sale " + saleNumber, sale.getTotalAmount(), "D",
                    saleNumber);
            createAccountingEntry("4100", "Sales Revenue", "Sale " + saleNumber, sale.getTotalAmount(), "C",
                    saleNumber);

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            // ROLLBACK STOCK if accounting fails
            if (originalStock != null) {
                try {
                    warehouseClient.updateStock(productId, new ProductUpdateRequest(originalStock));
                    System.out.println("STOCK ROLLBACK SUCCESSFUL: Reverted stock to " + originalStock);
                } catch (Exception rollbackError) {
                    System.err.println("STOCK ROLLBACK FAILED: " + rollbackError.getMessage());
                }
            }
            return ResponseEntity.badRequest().build();
        }
    }

    private void createAccountingEntry(String accountCode, String accountName, String description, BigDecimal amount,
            String balanceType, String referenceNumber) {
        AccountingRequest request = new AccountingRequest();
        request.accountCode = accountCode;
        request.accountName = accountName;
        request.description = description;
        request.amount = amount;
        request.balanceType = balanceType;
        request.referenceNumber = referenceNumber;
        request.createdBy = "SALES_SERVICE";

        try {
            restTemplate.postForObject("http://localhost:8080/api/accounting/journal", request, Object.class);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Accounting service failed: " + e.getMessage());
        }
    }
}

class AccountingRequest {
    public String accountCode;
    public String accountName;
    public String description;
    public BigDecimal amount;
    public String balanceType;
    public String referenceNumber;
    public String createdBy;
}