import java.time.LocalDateTime;

public class ArtItems extends Item {
    private String author ; 
    private String typeProduct ; 
    public ArtItems(String name, String description, double startPrice, LocalDateTime startTime, LocalDateTime endTime,  String sellerId, String author, String typeProduct){
        super(name, description, startPrice, startTime, endTime, sellerId) ;
        this.author = author; 
        this.typeProduct = typeProduct ; 
    }

    public String getAuthor() {return this.author;}
    public String getTypeProduct() {return this.typeProduct;}
    @Override
    public String getDetails(){
        return String.format("Sản phẩm nghệ thuật : %s - Loại : %s - Giá hiện tại : %.2f - Tác giả : %s", getName(), typeProduct,getCurrentHighestPrice(), author);
    }
}
