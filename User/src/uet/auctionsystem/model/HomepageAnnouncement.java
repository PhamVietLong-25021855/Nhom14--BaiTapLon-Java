package uet.auctionsystem.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop HomepageAnnouncement; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class HomepageAnnouncement {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho id.
    private int id;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho title.
    private String title;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho summary.
    private String summary;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho details.
    private String details;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho schedule text.
    private String scheduleText;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho linked auction id.
    private int linkedAuctionId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho author id.
    private int authorId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho created at.
    private long createdAt;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho updated at.
    private long updatedAt;
    // Ham tao: khoi tao doi tuong HomepageAnnouncement voi cac phu thuoc can thiet.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac get id.
    public int getId() {
        return id;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set id.
    public void setId(int id) {
        this.id = id;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get title.
    public String getTitle() {
        return title;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set title.
    public void setTitle(String title) {
        this.title = title;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get summary.
    public String getSummary() {
        return summary;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set summary.
    public void setSummary(String summary) {
        this.summary = summary;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get details.
    public String getDetails() {
        return details;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set details.
    public void setDetails(String details) {
        this.details = details;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get schedule text.
    public String getScheduleText() {
        return scheduleText;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set schedule text.
    public void setScheduleText(String scheduleText) {
        this.scheduleText = scheduleText;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get linked auction id.
    public int getLinkedAuctionId() {
        return linkedAuctionId;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set linked auction id.
    public void setLinkedAuctionId(int linkedAuctionId) {
        this.linkedAuctionId = linkedAuctionId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get author id.
    public int getAuthorId() {
        return authorId;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set author id.
    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get created at.
    public long getCreatedAt() {
        return createdAt;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set created at.
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get updated at.
    public long getUpdatedAt() {
        return updatedAt;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set updated at.
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac has linked auction.
    public boolean hasLinkedAuction() {
        return linkedAuctionId > 0;
    }
}
