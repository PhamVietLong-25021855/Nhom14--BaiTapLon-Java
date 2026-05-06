package userauth.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop AutoBid; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class AutoBid extends Entity {
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final int auctionId;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final int bidderId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho max price.
    private double maxPrice;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho increment.
    private double increment;
    // Timestamp giÃºp á»•n Ä‘á»‹nh thá»© tá»± Æ°u tiÃªn khi nhiá»u luáº­t auto-bid cÃ¹ng cáº¡nh tranh.
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final long createdAt;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho updated at.
    private long updatedAt;
    // Ham tao: khoi tao doi tuong AutoBid voi cac phu thuoc can thiet.
    public AutoBid(int id, int auctionId, int bidderId, double maxPrice, double increment) {
        this(id, auctionId, bidderId, maxPrice, increment, System.currentTimeMillis(), System.currentTimeMillis());
    }
    // Ham tao: khoi tao doi tuong AutoBid voi cac phu thuoc can thiet.
    public AutoBid(int id, int auctionId, int bidderId, double maxPrice, double increment, long createdAt, long updatedAt) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxPrice = maxPrice;
        this.increment = increment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get auction id.
    public int getAuctionId() {
        return auctionId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get bidder id.
    public int getBidderId() {
        return bidderId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get max price.
    public double getMaxPrice() {
        return maxPrice;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get increment.
    public double getIncrement() {
        return increment;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get created at.
    public long getCreatedAt() {
        return createdAt;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get updated at.
    public long getUpdatedAt() {
        return updatedAt;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set increment.
    public void setIncrement(double increment) {
        this.increment = increment;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set max price.
    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set updated at.
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
