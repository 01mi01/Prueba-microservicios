package microservices.sales.client;

public class ProductUpdateRequest {
    private Integer stockQuantity;
    
    public ProductUpdateRequest() {}
    
    public ProductUpdateRequest(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
}