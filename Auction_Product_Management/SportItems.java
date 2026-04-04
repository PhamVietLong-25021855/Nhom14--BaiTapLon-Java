import java.time.LocalDateTime;

public class SportItems extends Item {
    private String brand ; 
    private String typeProduct ;
    
    public SportItems(String name, String description, double startPrice, LocalDateTime startTime, LocalDateTime endTime,  String sellerId, String brand , String typeProduct){
        super(name, description, startPrice, startTime, endTime, sellerId) ; 
        this.brand = brand; 
        this.typeProduct = typeProduct ; 
    }

    public String getBrand() {return this.brand ; }
    public String getTypeProduct() {return this.typeProduct ; }
    @Override 
    public String getDetails() {
        return String.format("Sản phẩm thể thao : %s - Giá hiện tại : %.2f - Loại : %s - Thương Hiệu : %s", getName(), getCurrentHighestPrice() ,typeProduct , brand);
    }
}
