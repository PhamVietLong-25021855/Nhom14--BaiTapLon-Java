package userauth.model;

public class BidTransaction extends Entity {
    private int auctionId;
    private int bidderId;
    private double amount;
    private long timestamp;
    private String status;

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

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return id + "," + auctionId + "," + bidderId + "," + amount + "," + timestamp + "," + status;
    }
}
