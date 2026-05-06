package userauth.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop Item; mo ta cau truc du lieu cua doi tuong nghiep vu.
public abstract class Item extends Entity {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho name.
    protected String name;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho description.
    protected String description;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho start price.
    protected double startPrice;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho current highest bid.
    protected double currentHighestBid;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho start time.
    protected long startTime;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho end time.
    protected long endTime;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho category.
    protected String category;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho image source.
    protected String imageSource;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho image data.
    protected byte[] imageData;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho created at.
    protected long createdAt;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho updated at.
    protected long updatedAt;
    // Ham tao: khoi tao doi tuong Item voi cac phu thuoc can thiet.
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
    // Ham tao: khoi tao doi tuong Item voi cac phu thuoc can thiet.
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
    // Phuong thuc: lay hoac doc du lieu cho thao tac get name.
    public String getName() { return name; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set name.
    public void setName(String name) { this.name = name; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get description.
    public String getDescription() { return description; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set description.
    public void setDescription(String description) { this.description = description; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get start price.
    public double getStartPrice() { return startPrice; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set start price.
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get current highest bid.
    public double getCurrentHighestBid() { return currentHighestBid; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set current highest bid.
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get start time.
    public long getStartTime() { return startTime; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set start time.
    public void setStartTime(long startTime) { this.startTime = startTime; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get end time.
    public long getEndTime() { return endTime; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set end time.
    public void setEndTime(long endTime) { this.endTime = endTime; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get category.
    public String getCategory() { return category; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set category.
    public void setCategory(String category) { this.category = category; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get image source.
    public String getImageSource() { return imageSource; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set image source.
    public void setImageSource(String imageSource) { this.imageSource = imageSource; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get image data.
    public byte[] getImageData() { return imageData; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set image data.
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get created at.
    public long getCreatedAt() { return createdAt; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set created at.
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get updated at.
    public long getUpdatedAt() { return updatedAt; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set updated at.
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
