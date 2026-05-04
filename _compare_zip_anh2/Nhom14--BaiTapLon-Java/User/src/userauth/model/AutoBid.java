package userauth.model;

public class AutoBid extends Entity {
    private int auctionId;
    private int bidderId;
    private double maxPrice;
    private double increment;
    public AutoBid(int id, int auctionId, int bidderId, double maxPrice, double increment) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxPrice = maxPrice;
        this.increment = increment;
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

    public void setIncrement(double increment) {
        this.increment = increment;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }
}
