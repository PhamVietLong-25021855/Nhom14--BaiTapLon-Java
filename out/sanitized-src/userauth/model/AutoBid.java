package userauth.model;

// File note: Model luật auto-bid của bidder trên một auction cụ thể.
// LÆ°u luáº­t auto-bid cá»§a bidder cho má»™t auction cá»¥ thá»ƒ.
public class AutoBid extends Entity {
    private final int auctionId;
    private final int bidderId;
    private double maxPrice;
    private double increment;
    // Timestamp giÃºp á»•n Ä‘á»‹nh thá»© tá»± Æ°u tiÃªn khi nhiá»u luáº­t auto-bid cÃ¹ng cáº¡nh tranh.
    private final long createdAt;
    private long updatedAt;

    public AutoBid(int id, int auctionId, int bidderId, double maxPrice, double increment) {
        this(id, auctionId, bidderId, maxPrice, increment, System.currentTimeMillis(), System.currentTimeMillis());
    }

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

