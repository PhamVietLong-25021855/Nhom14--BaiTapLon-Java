import java.time.LocalDateTime;
public abstract class Item extends Entity{
    private String name ; 
    private String description ; 
    private double startPrice ; 
    private double currentHighestPrice ; 
    private LocalDateTime startTime ; 
    private LocalDateTime endTime ; 
    private String sellerId ; 
    private ItemStatus status ; 

    public Item(String name , String description , double startPrice , LocalDateTime startTime , LocalDateTime endTime , String sellerId){
        this.setName(name) ; 
        this.setDescription(description);
        this.setStartPrice(startPrice);
        this.currentHighestPrice = startPrice ; 
        this.setAuctionTime(startTime , endTime) ;
        this.sellerId = sellerId ; 
        this.status = ItemStatus.PENDING ; 
    }

    public void setName(String name){
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Name cannot be empty !!!");
        }
        this.name = name ; 
    }

    public void setDescription(String description){
        if(description == null || description.trim().isEmpty()){
            throw new IllegalArgumentException("Description cannot be empty!!!");
        }
    }

    public void setStartPrice(double price){
        if(price <= 0){
            throw new IllegalArgumentException("The price must be greater than 0 !!!");
        }
        this.startPrice = price ; 
    }

    public void setAuctionTime(LocalDateTime startTime , LocalDateTime endTime){
        if(endTime.isBefore(startTime)){
            throw new IllegalArgumentException("The endtime must be occur after the start time !!!");
        }
        this.startTime = startTime ; 
        this.endTime = endTime ; 
    }

    public String getName() {return name;}
    public String getDescription() {return description;}
    public double getStartPrice() {return startPrice;}
    public double getCurrentHighestPrice() {return currentHighestPrice;}
    public LocalDateTime getStartTime() {return startTime;} 
    public LocalDateTime getEndTime() {return endTime;}
    public String getSellerId() {return sellerId;} 
    public ItemStatus getItemStatus() {return status;}
    
    public void setStatus(ItemStatus status){
        this.status = status ; 
    }

    public abstract String getDetails();
    
}

