package microservices.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
        Sale savedSale = null;
        
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

            // 3. Create accounting entries FIRST (THIS WILL FAIL IF testPrice = 0.99)
            String saleNumber = "SALE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            BigDecimal unitPrice = testPrice != null ? testPrice : product.getPrice();
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            
            createAccountingEntry("1200", "Accounts Receivable", "Sale " + saleNumber, totalAmount, "D", saleNumber);
            createAccountingEntry("4100", "Sales Revenue", "Sale " + saleNumber, totalAmount, "C", saleNumber);

            // 4. Create sale ONLY if accounting succeeded
            Sale sale = new Sale(saleNumber, productId, quantity, unitPrice);
            sale.setTotalAmount(totalAmount);
            savedSale = saleRepository.save(sale);
            
            System.out.println("TRANSACTION SUCCESSFUL: Sale " + saleNumber + " completed");
            return ResponseEntity.ok(savedSale);

        } catch (Exception e) {
            System.err.println("TRANSACTION FAILED: " + e.getMessage());
            
            // ROLLBACK STOCK if accounting fails
            if (originalStock != null) {
                try {
                    warehouseClient.updateStock(productId, new ProductUpdateRequest(originalStock));
                    System.out.println("STOCK ROLLBACK SUCCESSFUL: Reverted stock to " + originalStock);
                } catch (Exception rollbackError) {
                    System.err.println("STOCK ROLLBACK FAILED: " + rollbackError.getMessage());
                }
            }
            
            // DELETE SALE if it was saved
            if (savedSale != null) {
                try {
                    saleRepository.delete(savedSale);
                    System.out.println("SALE ROLLBACK SUCCESSFUL: Deleted sale " + savedSale.getSaleNumber());
                } catch (Exception rollbackError) {
                    System.err.println("SALE ROLLBACK FAILED: " + rollbackError.getMessage());
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
        } catch (Exception e) {
            throw new RuntimeException("Accounting service error: " + e.getMessage());
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