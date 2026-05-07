package userauth.model;

/**
 * Lớp cha trừu tượng cho các đối tượng/sản phẩm có thể được đưa vào đấu giá.
 *
 * Item lưu những thông tin chung của một sản phẩm:
 * tên, mô tả, giá khởi điểm, giá hiện tại, thời gian bắt đầu/kết thúc,
 * danh mục, ảnh và thời gian tạo/cập nhật.
 * AuctionItem kế thừa Item để bổ sung seller, winner và trạng thái đấu giá.
 */
public abstract class Item extends Entity {
    /** Tên sản phẩm/phiên đấu giá. */
    protected String name;

    /** Mô tả chi tiết sản phẩm. */
    protected String description;

    /** Giá khởi điểm của sản phẩm. */
    protected double startPrice;

    /** Giá cao nhất hiện tại trong phiên đấu giá. */
    protected double currentHighestBid;

    /** Thời gian bắt đầu phiên đấu giá, lưu dạng millisecond. */
    protected long startTime;

    /** Thời gian kết thúc phiên đấu giá, lưu dạng millisecond. */
    protected long endTime;

    /** Danh mục sản phẩm, ví dụ: đồ điện tử, thời trang, sưu tầm. */
    protected String category;

    /** Đường dẫn hoặc nguồn ảnh của sản phẩm. */
    protected String imageSource;

    /** Dữ liệu ảnh dạng byte, dùng khi lưu ảnh trực tiếp vào database hoặc bộ nhớ. */
    protected byte[] imageData;

    /** Thời điểm tạo bản ghi sản phẩm/phiên đấu giá. */
    protected long createdAt;

    /** Thời điểm cập nhật gần nhất. */
    protected long updatedAt;

    /**
     * Constructor đầy đủ, dùng khi cần truyền cả thông tin ảnh.
     *
     * @param id id sản phẩm
     * @param name tên sản phẩm
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
     */
    public Item(
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
            long updatedAt
    ) {
        super(id);
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.currentHighestBid = currentHighestBid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.imageSource = imageSource;
        this.imageData = imageData;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Constructor rút gọn khi sản phẩm chưa có ảnh hoặc không cần truyền ảnh.
     * imageSource và imageData sẽ được gán null.
     */
    public Item(
            int id,
            String name,
            String description,
            double startPrice,
            double currentHighestBid,
            long startTime,
            long endTime,
            String category,
            long createdAt,
            long updatedAt
    ) {
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, null, null, createdAt, updatedAt);
    }

    /** @return tên sản phẩm */
    public String getName() { return name; }

    /** @param name tên sản phẩm mới */
    public void setName(String name) { this.name = name; }

    /** @return mô tả sản phẩm */
    public String getDescription() { return description; }

    /** @param description mô tả mới */
    public void setDescription(String description) { this.description = description; }

    /** @return giá khởi điểm */
    public double getStartPrice() { return startPrice; }

    /** @param startPrice giá khởi điểm mới */
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }

    /** @return giá cao nhất hiện tại */
    public double getCurrentHighestBid() { return currentHighestBid; }

    /** @param currentHighestBid giá cao nhất hiện tại cần cập nhật */
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    /** @return thời gian bắt đầu phiên đấu giá */
    public long getStartTime() { return startTime; }

    /** @param startTime thời gian bắt đầu mới */
    public void setStartTime(long startTime) { this.startTime = startTime; }

    /** @return thời gian kết thúc phiên đấu giá */
    public long getEndTime() { return endTime; }

    /** @param endTime thời gian kết thúc mới */
    public void setEndTime(long endTime) { this.endTime = endTime; }

    /** @return danh mục sản phẩm */
    public String getCategory() { return category; }

    /** @param category danh mục mới */
    public void setCategory(String category) { this.category = category; }

    /** @return nguồn hoặc đường dẫn ảnh */
    public String getImageSource() { return imageSource; }

    /** @param imageSource nguồn hoặc đường dẫn ảnh mới */
    public void setImageSource(String imageSource) { this.imageSource = imageSource; }

    /** @return dữ liệu ảnh dạng byte */
    public byte[] getImageData() { return imageData; }

    /** @param imageData dữ liệu ảnh mới */
    public void setImageData(byte[] imageData) { this.imageData = imageData; }

    /** @return thời điểm tạo */
    public long getCreatedAt() { return createdAt; }

    /** @param createdAt thời điểm tạo cần gán */
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** @return thời điểm cập nhật gần nhất */
    public long getUpdatedAt() { return updatedAt; }

    /** @param updatedAt thời điểm cập nhật mới */
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
