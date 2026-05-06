package userauth.model;

/**
 * Model đại diện cho thông báo hiển thị ở trang chủ.
 *
 * Thông báo có thể là tin giới thiệu, lịch đấu giá nổi bật hoặc thông báo từ admin.
 * Nếu linkedAuctionId > 0, thông báo được liên kết với một phiên đấu giá cụ thể
 * để người dùng bấm vào và xem chi tiết phiên đó.
 */
public class HomepageAnnouncement {
    /** Id thông báo. */
    private int id;

    /** Tiêu đề ngắn của thông báo. */
    private String title;

    /** Tóm tắt ngắn hiển thị trên card/trang chủ. */
    private String summary;

    /** Nội dung chi tiết của thông báo. */
    private String details;

    /** Chuỗi mô tả lịch/sự kiện, ví dụ thời gian diễn ra phiên đấu giá. */
    private String scheduleText;

    /** Id phiên đấu giá được liên kết; nếu <= 0 nghĩa là không liên kết phiên nào. */
    private int linkedAuctionId;

    /** Id người tạo thông báo, thường là admin. */
    private int authorId;

    /** Thời điểm tạo thông báo. */
    private long createdAt;

    /** Thời điểm cập nhật thông báo gần nhất. */
    private long updatedAt;

    /**
     * Constructor tạo một thông báo trang chủ.
     *
     * @param id id thông báo
     * @param title tiêu đề
     * @param summary tóm tắt
     * @param details nội dung chi tiết
     * @param scheduleText thông tin lịch hiển thị
     * @param linkedAuctionId id phiên đấu giá liên kết
     * @param authorId id người tạo thông báo
     * @param createdAt thời điểm tạo
     * @param updatedAt thời điểm cập nhật
     */
    public HomepageAnnouncement(int id, String title, String summary, String details, String scheduleText,
                                int linkedAuctionId, int authorId, long createdAt, long updatedAt) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.details = details;
        this.scheduleText = scheduleText;
        this.linkedAuctionId = linkedAuctionId;
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** @return id thông báo */
    public int getId() {
        return id;
    }

    /** @param id id thông báo mới */
    public void setId(int id) {
        this.id = id;
    }

    /** @return tiêu đề thông báo */
    public String getTitle() {
        return title;
    }

    /** @param title tiêu đề mới */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return nội dung tóm tắt */
    public String getSummary() {
        return summary;
    }

    /** @param summary nội dung tóm tắt mới */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /** @return nội dung chi tiết */
    public String getDetails() {
        return details;
    }

    /** @param details nội dung chi tiết mới */
    public void setDetails(String details) {
        this.details = details;
    }

    /** @return chuỗi lịch/thời gian hiển thị */
    public String getScheduleText() {
        return scheduleText;
    }

    /** @param scheduleText chuỗi lịch/thời gian mới */
    public void setScheduleText(String scheduleText) {
        this.scheduleText = scheduleText;
    }

    /** @return id phiên đấu giá liên kết */
    public int getLinkedAuctionId() {
        return linkedAuctionId;
    }

    /** @param linkedAuctionId id phiên đấu giá liên kết mới */
    public void setLinkedAuctionId(int linkedAuctionId) {
        this.linkedAuctionId = linkedAuctionId;
    }

    /** @return id tác giả tạo thông báo */
    public int getAuthorId() {
        return authorId;
    }

    /** @param authorId id tác giả mới */
    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    /** @return thời điểm tạo thông báo */
    public long getCreatedAt() {
        return createdAt;
    }

    /** @param createdAt thời điểm tạo mới */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /** @return thời điểm cập nhật gần nhất */
    public long getUpdatedAt() {
        return updatedAt;
    }

    /** @param updatedAt thời điểm cập nhật mới */
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Kiểm tra thông báo có liên kết với phiên đấu giá nào không.
     *
     * @return true nếu linkedAuctionId > 0, false nếu không có liên kết
     */
    public boolean hasLinkedAuction() {
        return linkedAuctionId > 0;
    }
}
