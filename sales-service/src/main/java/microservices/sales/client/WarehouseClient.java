package microservices.sales.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "warehouse-service")
public interface WarehouseClient {
    
    @GetMapping("/api/warehouse/products/{id}")
    ProductResponse getProduct(@PathVariable("id") Integer id);
    
    @PutMapping("/api/warehouse/products/{id}/stock")
    ProductResponse updateStock(@PathVariable("id") Integer id, @RequestBody ProductUpdateRequest product);
}