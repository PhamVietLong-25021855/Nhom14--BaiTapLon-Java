package userauth.model;

public class AuctionItem extends Item {
    private int sellerId;
    private int winnerId; // -1 means no winner yet
    private AuctionStatus status; 

    public AuctionItem(int id, String name, String description, double startPrice, double currentHighestBid, long startTime, long endTime, String category, long createdAt, long updatedAt, int sellerId, int winnerId, AuctionStatus status) {
        super(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, createdAt, updatedAt);
        this.sellerId = sellerId;
        this.winnerId = winnerId;
        this.status = status;
    }

    public AuctionItem(int id, String name, String description, double startPrice, long startTime, long endTime, String category, int sellerId) {
        this(id, name, description, startPrice, startPrice, startTime, endTime, category, System.currentTimeMillis(), System.currentTimeMillis(), sellerId, -1, AuctionStatus.OPEN);
    }

    // Getter / Setter (Encapsulation)
    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    
    public int getWinnerId() { return winnerId; }
    public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
    
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    // Polymorphism: override printInfo() từ Entity → Item
    @Override
    public void printInfo() {
        System.out.println("=== SẢN PHẨM ĐẤU GIÁ ===");
        System.out.println("ID: " + id);
        System.out.println("Tên: " + name);
        System.out.println("Mô tả: " + description);
        System.out.println("Danh mục: " + category);
        System.out.println("Giá khởi điểm: " + startPrice);
        System.out.println("Giá cao nhất hiện tại: " + currentHighestBid);
        System.out.println("Trạng thái: " + status);
        System.out.println("ID Người bán: " + sellerId);
        System.out.println("ID Người thắng: " + (winnerId == -1 ? "Chưa có" : winnerId));
    }

    @Override
    public String toString() {
        return id + "," + name + "," + description + "," + startPrice + "," + currentHighestBid + "," + startTime + "," + endTime + "," + category + "," + createdAt + "," + updatedAt + "," + sellerId + "," + winnerId + "," + status.name();
    }
}
