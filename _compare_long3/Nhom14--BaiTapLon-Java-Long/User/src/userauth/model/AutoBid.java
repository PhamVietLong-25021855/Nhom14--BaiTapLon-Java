package userauth.model;

public class AutoBid extends Entity {
    private int auctionId;
    private int bidderId;
    private double maxPrice;
    private double increment;
    private long createdAt;
    private long updatedAt;

    public AutoBid(int id, int auctionId, int bidderId, double maxPrice, double increment, long createdAt, long updatedAt) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxPrice = maxPrice;
        this.increment = increment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public double getIncrement() {
        return increment;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
