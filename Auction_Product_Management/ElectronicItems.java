import java.time.LocalDateTime;

public class ElectronicItems extends Item {
    private int warrantyMonths ; 
    private String typeProduct ;
    public ElectronicItems(String name, String description, double startPrice, LocalDateTime startTime, LocalDateTime endTime,  String sellerId, int warrantyMonths,String typeProduct){
        super(name, description, startPrice, startTime, endTime, sellerId); 
        this.warrantyMonths = warrantyMonths ; 
        this.typeProduct = typeProduct ; 
    }

    public int getwarrantyMonths() {return this.warrantyMonths;}
    public String getTypeProduct() {return this.typeProduct ;}
    @Override 
    public String getDetails(){
        return String.format("Sản phẩm điện tử : %s - Loại : %s - Giá hiện tại : %.2f - Bảo hành : %d tháng" , getName(),typeProduct, getCurrentHighestPrice() , warrantyMonths);
    }
}
