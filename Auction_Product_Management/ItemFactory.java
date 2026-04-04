import java.time.LocalDateTime;
import java.util.Map;

public class ItemFactory {
    
    public static Item createItem(String name, String description, double startPrice, 
                                  LocalDateTime startTime, LocalDateTime endTime,  
                                  String sellerId, Map<String, Object> extraParams, 
                                  ItemType type) {
        
        // Khai báo typeProduct ở ngoài để dùng chung cho tất cả các case
        String typeProduct = (String) extraParams.getOrDefault("typeProduct", "");
        
        switch(type) {
            case ELECTRONIC:
                int warranty = (int) extraParams.getOrDefault("warrantyMonths", 0);
                return new ElectronicItems(name, description, startPrice, startTime, 
                                           endTime, sellerId, warranty, typeProduct);
                
            case SPORT: // Đảm bảo tên Enum khớp
                String brand = (String) extraParams.getOrDefault("brand", "");
                // Gọi đúng class SportsItem
                return new SportItems(name, description, startPrice, startTime, 
                                      endTime, sellerId, brand, typeProduct);
                
            case ART:
                String author = (String) extraParams.getOrDefault("author", "");
                return new ArtItems(name, description, startPrice, startTime, 
                                   endTime, sellerId, author, typeProduct);
                
            default:
                throw new IllegalArgumentException("Product not existed !!!");
        }
    }
}