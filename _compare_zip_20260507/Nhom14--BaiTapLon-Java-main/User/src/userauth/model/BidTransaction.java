package userauth.model;

/**
 * Model đại diện cho một giao dịch đặt giá trong phiên đấu giá.
 *
 * Mỗi khi bidder đặt giá thủ công hoặc hệ thống auto-bid đặt giá tự động,
 * hệ thống sẽ tạo một BidTransaction để lưu lại lịch sử.
 * BidTransaction giúp truy vết ai đặt giá, đặt ở phiên nào, số tiền bao nhiêu,
 * thời điểm nào và giao dịch đó có được chấp nhận hay không.
 */
public class BidTransaction extends Entity {
    /** Id phiên đấu giá mà giao dịch thuộc về. */
    private int auctionId;

    /** Id bidder thực hiện đặt giá. */
    private int bidderId;

    /** Số tiền bidder đặt trong giao dịch này. */
    private double amount;

    /** Thời điểm đặt giá, lưu dạng millisecond. */
    private long timestamp;

    /** Trạng thái giao dịch, ví dụ ACCEPTED hoặc REJECTED. */
    private String status;

    /**
     * Constructor đầy đủ cho một giao dịch đặt giá.
     *
     * @param id id giao dịch
     * @param auctionId id phiên đấu giá
     * @param bidderId id người đặt giá
     * @param amount số tiền đặt giá
     * @param timestamp thời điểm đặt giá
     * @param status trạng thái giao dịch
     */
    public BidTransaction(int id, int auctionId, int bidderId, double amount, long timestamp, String status) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }

    /**
     * Constructor rút gọn khi giao dịch mặc định được chấp nhận.
     * status sẽ tự động là ACCEPTED.
     */
    public BidTransaction(int id, int auctionId, int bidderId, double amount, long timestamp) {
        this(id, auctionId, bidderId, amount, timestamp, "ACCEPTED");
    }

    /** @return id phiên đấu giá */
    public int getAuctionId() {
        return auctionId;
    }

    /** @param auctionId id phiên đấu giá mới */
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    /** @return id bidder đặt giá */
    public int getBidderId() {
        return bidderId;
    }

    /** @param bidderId id bidder mới */
    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }

    /** @return số tiền đặt giá */
    public double getAmount() {
        return amount;
    }

    /** @param amount số tiền đặt giá mới */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /** @return thời điểm đặt giá */
    public long getTimestamp() {
        return timestamp;
    }

    /** @param timestamp thời điểm đặt giá mới */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /** @return trạng thái giao dịch */
    public String getStatus() {
        return status;
    }

    /** @param status trạng thái giao dịch mới */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Chuyển giao dịch đặt giá thành chuỗi ngăn cách bởi dấu phẩy.
     *
     * Hàm này hữu ích khi debug hoặc ghi log lịch sử đặt giá.
     *
     * @return chuỗi chứa thông tin giao dịch đặt giá
     */
    @Override
    public String toString() {
        return id + "," + auctionId + "," + bidderId + "," + amount + "," + timestamp + "," + status;
    }
}
