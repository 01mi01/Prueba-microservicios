package microservices.warehouse.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import microservices.warehouse.entity.Product;
import microservices.warehouse.repository.ProductRepository;
import java.util.Optional;

@RestController
@RequestMapping("/api/warehouse")
public class ProductController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Integer id) {
        try {
            System.out.println("Getting product with ID: " + id);
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                System.out.println("Found product: " + product.getName());
                return ResponseEntity.ok(product);
            } else {
                System.out.println("Product not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("ERROR in getProduct: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/products/{id}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable Integer id, @RequestBody StockUpdateRequest request) {
        try {
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setStockQuantity(request.getStockQuantity());
                Product saved = productRepository.save(product);
                System.out.println("Updated stock for product " + id + " to " + request.getStockQuantity());
                return ResponseEntity.ok(saved);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("ERROR in updateStock: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}

class StockUpdateRequest {
    private Integer stockQuantity;
    
    public StockUpdateRequest() {}
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
}