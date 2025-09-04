package microservices.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    
    @PostMapping
    public ResponseEntity<Sale> createSale(@RequestParam Integer productId, @RequestParam Integer quantity) {
        try {
            // Get product from warehouse service
            ProductResponse product = warehouseClient.getProduct(productId);
            
            if (product.getStockQuantity() < quantity) {
                return ResponseEntity.badRequest().build();
            }
            
            // Create sale
            String saleNumber = "SALE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Sale sale = new Sale(saleNumber, productId, quantity, product.getPrice());
            sale.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            
            // Update stock
            int newStock = product.getStockQuantity() - quantity;
            warehouseClient.updateStock(productId, new ProductUpdateRequest(newStock));
            
            // Save sale
            Sale saved = saleRepository.save(sale);
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}