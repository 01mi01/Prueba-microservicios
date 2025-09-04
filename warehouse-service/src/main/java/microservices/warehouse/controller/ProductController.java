package microservices.warehouse.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import microservices.warehouse.entity.Product;
import microservices.warehouse.repository.ProductRepository;

@RestController
@RequestMapping("/api/warehouse")
public class ProductController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Integer id) {
        return productRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/products/{id}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable Integer id, @RequestBody Product product) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        product.setId(id);
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(saved);
    }
}