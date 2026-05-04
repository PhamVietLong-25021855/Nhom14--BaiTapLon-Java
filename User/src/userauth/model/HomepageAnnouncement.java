package userauth.model;

public class HomepageAnnouncement {
    private int id;
    private String title;
    private String summary;
    private String details;
    private String scheduleText;
    private int linkedAuctionId;
    private int authorId;
    private long createdAt;
    private long updatedAt;

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getScheduleText() {
        return scheduleText;
    }

    public void setScheduleText(String scheduleText) {
        this.scheduleText = scheduleText;
    }

    public int getLinkedAuctionId() {
        return linkedAuctionId;
    }

    public void setLinkedAuctionId(int linkedAuctionId) {
        this.linkedAuctionId = linkedAuctionId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean hasLinkedAuction() {
        return linkedAuctionId > 0;
    }
}
