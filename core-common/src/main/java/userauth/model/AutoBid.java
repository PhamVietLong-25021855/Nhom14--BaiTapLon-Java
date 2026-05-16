package userauth.model;

/**
 * Model lưu cấu hình đấu giá tự động của một bidder trong một phiên đấu giá.
 *
 * Khi bidder không muốn tự đặt giá nhiều lần, bidder có thể đặt:
 * - maxPrice: mức giá tối đa chấp nhận trả.
 * - increment: bước tăng giá mỗi lần hệ thống tự đặt.
 *
 * AuctionService sẽ đọc các AutoBid liên quan đến một AuctionItem và tự tạo
 * BidTransaction mới nếu điều kiện đấu giá tự động phù hợp.
 */
public class AutoBid extends Entity {
    /** Id phiên đấu giá mà cấu hình auto-bid áp dụng. */
    private int auctionId;

    /** Id của bidder thiết lập auto-bid. */
    private int bidderId;

    /** Mức giá tối đa bidder chấp nhận trả. */
    private double maxPrice;

    /** Bước tăng giá tự động mỗi lần hệ thống cần đẩy giá lên. */
    private double increment;

    /** Thời điểm tạo cấu hình auto-bid. */
    private long createdAt;

    /** Thời điểm cập nhật cấu hình auto-bid gần nhất. */
    private long updatedAt;

    /**
     * Constructor tạo một cấu hình đấu giá tự động.
     *
     * @param id id cấu hình auto-bid
     * @param auctionId id phiên đấu giá
     * @param bidderId id người đặt auto-bid
     * @param maxPrice giá tối đa bidder chấp nhận
     * @param increment bước tăng giá tự động
     * @param createdAt thời điểm tạo
     * @param updatedAt thời điểm cập nhật
     */
    public AutoBid(int id, int auctionId, int bidderId, double maxPrice, double increment, long createdAt, long updatedAt) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxPrice = maxPrice;
        this.increment = increment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** @return id phiên đấu giá áp dụng auto-bid */
    public int getAuctionId() {
        return auctionId;
    }

    /** @return id bidder thiết lập auto-bid */
    public int getBidderId() {
        return bidderId;
    }

    /** @return giá tối đa bidder chấp nhận trả */
    public double getMaxPrice() {
        return maxPrice;
    }

    /** @return bước tăng giá tự động */
    public double getIncrement() {
        return increment;
    }

    /** @return thời điểm tạo cấu hình */
    public long getCreatedAt() {
        return createdAt;
    }

    /** @return thời điểm cập nhật gần nhất */
    public long getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Cập nhật bước tăng giá tự động.
     *
     * @param increment bước tăng mới
     */
    public void setIncrement(double increment) {
        this.increment = increment;
    }

    /**
     * Cập nhật mức giá tối đa cho auto-bid.
     *
     * @param maxPrice mức giá tối đa mới
     */
    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    /**
     * Cập nhật thời điểm sửa cấu hình auto-bid.
     *
     * @param updatedAt thời điểm cập nhật mới
     */
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
