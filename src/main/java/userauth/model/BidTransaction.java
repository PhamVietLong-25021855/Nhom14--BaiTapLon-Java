package userauth.model;

/**
 * Lớp BidTransaction kế thừa Entity.
 * Đại diện cho một giao dịch đặt giá trong hệ thống đấu giá.
 * Thể hiện: Inheritance (extends Entity), Encapsulation (private fields),
 *           Polymorphism (override printInfo)
 */
public class BidTransaction extends Entity {
    private int auctionId;
    private int bidderId;
    private double amount;
    private long timestamp;
    private String status; // ACCEPTED, REJECTED, WINNING

    public BidTransaction(int id, int auctionId, int bidderId, double amount, long timestamp, String status) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }

    public BidTransaction(int id, int auctionId, int bidderId, double amount, long timestamp) {
        this(id, auctionId, bidderId, amount, timestamp, "ACCEPTED");
    }

    // Getter / Setter (Encapsulation)
    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getBidderId() { return bidderId; }
    public void setBidderId(int bidderId) { this.bidderId = bidderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Polymorphism: override printInfo() từ Entity
    @Override
    public void printInfo() {
        System.out.println("=== GIAO DỊCH ĐẶT GIÁ ===");
        System.out.println("Mã giao dịch: " + id);
        System.out.println("Mã phiên đấu giá: " + auctionId);
        System.out.println("Mã người đặt: " + bidderId);
        System.out.println("Số tiền: " + amount);
        System.out.println("Thời gian: " + new java.util.Date(timestamp));
        System.out.println("Trạng thái: " + status);
    }

    @Override
    public String toString() {
        return id + "," + auctionId + "," + bidderId + "," + amount + "," + timestamp + "," + status;
    }
}
