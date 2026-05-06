package userauth.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop BidTransaction; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class BidTransaction extends Entity {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho auction id.
    private int auctionId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho bidder id.
    private int bidderId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho amount.
    private double amount;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho timestamp.
    private long timestamp;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho status.
    private String status;
    // Ham tao: khoi tao doi tuong BidTransaction voi cac phu thuoc can thiet.
    public BidTransaction(int id, int auctionId, int bidderId, double amount, long timestamp, String status) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }
    // Ham tao: khoi tao doi tuong BidTransaction voi cac phu thuoc can thiet.
    public BidTransaction(int id, int auctionId, int bidderId, double amount, long timestamp) {
        this(id, auctionId, bidderId, amount, timestamp, "ACCEPTED");
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get auction id.
    public int getAuctionId() {
        return auctionId;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auction id.
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get bidder id.
    public int getBidderId() {
        return bidderId;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set bidder id.
    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get amount.
    public double getAmount() {
        return amount;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set amount.
    public void setAmount(double amount) {
        this.amount = amount;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get timestamp.
    public long getTimestamp() {
        return timestamp;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set timestamp.
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get status.
    public String getStatus() {
        return status;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set status.
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    // Phuong thuc: thuc hien chuc nang to string trong lop BidTransaction.
    public String toString() {
        return id + "," + auctionId + "," + bidderId + "," + amount + "," + timestamp + "," + status;
    }
}
