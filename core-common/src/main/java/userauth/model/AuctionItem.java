package userauth.model;

/**
 * Model đại diện cho một phiên đấu giá cụ thể.
 *
 * AuctionItem kế thừa Item nên có sẵn thông tin sản phẩm như tên, mô tả,
 * giá khởi điểm, giá cao nhất, thời gian bắt đầu/kết thúc và ảnh.
 * Lớp này bổ sung các thông tin riêng của đấu giá:
 * - sellerId: người bán tạo phiên.
 * - winnerId: người thắng sau khi phiên kết thúc.
 * - status: trạng thái phiên đấu giá.
 * - antiSnipingExtensionCount: số lần gia hạn chống đặt giá phút cuối.
 */
public class AuctionItem extends Item {
    /** Id của seller/người bán tạo phiên đấu giá. */
    private int sellerId;

    /** Id của bidder thắng phiên đấu giá; thường là -1 hoặc null-equivalent nếu chưa có người thắng. */
    private int winnerId;

    /** Trạng thái hiện tại của phiên đấu giá. */
    private AuctionStatus status;

    /** Số lần phiên đấu giá đã được gia hạn do cơ chế chống sniping. */
    private int antiSnipingExtensionCount;

    /**
     * Constructor đầy đủ nhất, dùng khi đọc toàn bộ dữ liệu phiên đấu giá từ database.
     *
     * @param id id phiên đấu giá
     * @param name tên sản phẩm/phiên
     * @param description mô tả sản phẩm
     * @param startPrice giá khởi điểm
     * @param currentHighestBid giá cao nhất hiện tại
     * @param startTime thời gian bắt đầu
     * @param endTime thời gian kết thúc
     * @param category danh mục sản phẩm
     * @param imageSource nguồn/đường dẫn ảnh
     * @param imageData dữ liệu ảnh dạng byte
     * @param createdAt thời điểm tạo
     * @param updatedAt thời điểm cập nhật
     * @param sellerId id người bán
     * @param winnerId id người thắng
     * @param status trạng thái phiên đấu giá
     * @param antiSnipingExtensionCount số lần gia hạn chống sniping
     */
    public AuctionItem(
            int id,
            String name,
            String description,
            double startPrice,
            double currentHighestBid,
            long startTime,
            long endTime,
            String category,
            String imageSource,
            byte[] imageData,
            long createdAt,
            long updatedAt,
            int sellerId,
            int winnerId,
            AuctionStatus status,
            int antiSnipingExtensionCount
    ) {
        super(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, imageSource, imageData, createdAt, updatedAt);
        this.sellerId = sellerId;
        this.winnerId = winnerId;
        this.status = status;
        this.antiSnipingExtensionCount = antiSnipingExtensionCount;
    }

    /**
     * Constructor dùng khi chưa cần truyền số lần gia hạn anti-sniping.
     * antiSnipingExtensionCount sẽ mặc định bằng 0.
     */
    public AuctionItem(
            int id,
            String name,
            String description,
            double startPrice,
            double currentHighestBid,
            long startTime,
            long endTime,
            String category,
            String imageSource,
            byte[] imageData,
            long createdAt,
            long updatedAt,
            int sellerId,
            int winnerId,
            AuctionStatus status
    ) {
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, imageSource, imageData, createdAt, updatedAt, sellerId, winnerId, status, 0);
    }

    /**
     * Constructor rút gọn khi phiên đấu giá không có thông tin ảnh.
     * imageSource và imageData sẽ được gán null.
     */
    public AuctionItem(
            int id,
            String name,
            String description,
            double startPrice,
            double currentHighestBid,
            long startTime,
            long endTime,
            String category,
            long createdAt,
            long updatedAt,
            int sellerId,
            int winnerId,
            AuctionStatus status
    ) {
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, null, null, createdAt, updatedAt, sellerId, winnerId, status);
    }

    /**
     * Constructor dùng khi seller tạo phiên đấu giá mới.
     *
     * Luồng thường gặp:
     * 1. Seller nhập thông tin sản phẩm trên giao diện.
     * 2. Controller tạo AuctionItem bằng constructor này.
     * 3. AuctionService kiểm tra dữ liệu hợp lệ.
     * 4. AuctionDAO lưu phiên đấu giá vào database.
     *
     * Khi mới tạo:
     * - currentHighestBid = startPrice.
     * - createdAt/updatedAt = thời gian hiện tại.
     * - winnerId = -1 vì chưa có người thắng.
     * - status = OPEN.
     * - antiSnipingExtensionCount = 0.
     */
    public AuctionItem(
            int id,
            String name,
            String description,
            double startPrice,
            long startTime,
            long endTime,
            String category,
            String imageSource,
            byte[] imageData,
            int sellerId
    ) {
        this(
                id,
                name,
                description,
                startPrice,
                startPrice,
                startTime,
                endTime,
                category,
                imageSource,
                imageData,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                sellerId,
                -1,
                AuctionStatus.OPEN,
                0
        );
    }

    /**
     * Constructor rút gọn khi tạo phiên mới nhưng không truyền ảnh.
     */
    public AuctionItem(int id, String name, String description, double startPrice, long startTime, long endTime, String category, int sellerId) {
        this(id, name, description, startPrice, startTime, endTime, category, null, null, sellerId);
    }

    /** @return id người bán tạo phiên đấu giá */
    public int getSellerId() {
        return sellerId;
    }

    /** @param sellerId id người bán mới */
    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    /** @return id người thắng phiên đấu giá */
    public int getWinnerId() {
        return winnerId;
    }

    /** @param winnerId id người thắng cần cập nhật */
    public void setWinnerId(int winnerId) {
        this.winnerId = winnerId;
    }

    /** @return trạng thái hiện tại của phiên đấu giá */
    public AuctionStatus getStatus() {
        return status;
    }

    /** @param status trạng thái mới của phiên đấu giá */
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    /** @return số lần đã gia hạn do chống sniping */
    public int getAntiSnipingExtensionCount() {
        return antiSnipingExtensionCount;
    }

    /** @param antiSnipingExtensionCount số lần gia hạn mới cần gán */
    public void setAntiSnipingExtensionCount(int antiSnipingExtensionCount) {
        this.antiSnipingExtensionCount = antiSnipingExtensionCount;
    }

    /**
     * Chuyển phiên đấu giá thành chuỗi ngăn cách bởi dấu phẩy.
     *
     * Hàm này thường dùng cho debug/log/xuất dữ liệu đơn giản.
     * Với lưu trữ chính thức, hệ thống nên dùng DAO và database thay vì tự parse chuỗi này.
     *
     * @return chuỗi chứa thông tin chính của AuctionItem
     */
    @Override
    public String toString() {
        return id + "," + name + "," + description + "," + startPrice + "," + currentHighestBid + "," + startTime + "," + endTime + "," + category + "," + imageSource + "," + createdAt + "," + updatedAt + "," + sellerId + "," + winnerId + "," + status.name() + "," + antiSnipingExtensionCount;
    }
}
