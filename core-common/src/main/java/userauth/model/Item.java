package userauth.model;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startPrice;
    protected double currentHighestBid;
    protected long startTime;
    protected long endTime;
    protected String category;
    protected String imageSource;
    protected byte[] imageData;
    protected long createdAt;
    protected long updatedAt;

    public Item(int id, String name, String description, double startPrice, double currentHighestBid,
                long startTime, long endTime, String category, String imageSource, byte[] imageData,
                long createdAt, long updatedAt) {
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

    public Item(int id, String name, String description, double startPrice, double currentHighestBid,
                long startTime, long endTime, String category, long createdAt, long updatedAt) {
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, null, null, createdAt, updatedAt);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageSource() { return imageSource; }
    public void setImageSource(String imageSource) { this.imageSource = imageSource; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
