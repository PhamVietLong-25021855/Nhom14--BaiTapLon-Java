package uet.auctionsystem.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop AuctionItem; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class AuctionItem extends Item {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho seller id.
    private int sellerId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho winner id.
    private int winnerId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho status.
    private AuctionStatus status;
    // Äáº¿m sá»‘ láº§n Ä‘Ã£ Ä‘Æ°á»£c gia háº¡n á»Ÿ cuá»‘i phiÃªn vÃ¬ anti-sniping.
    // Thuoc tinh: luu trang thai hoac du lieu tam cho anti sniping extension count.
    private int antiSnipingExtensionCount;
    // Ham tao: khoi tao doi tuong AuctionItem voi cac phu thuoc can thiet.
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
    // Ham tao: khoi tao doi tuong AuctionItem voi cac phu thuoc can thiet.
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
    // Ham tao: khoi tao doi tuong AuctionItem voi cac phu thuoc can thiet.
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
            long createdAt,
            long updatedAt,
            int sellerId,
            int winnerId,
            AuctionStatus status
    ) {
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, imageSource, null, createdAt, updatedAt, sellerId, winnerId, status);
    }
    // Ham tao: khoi tao doi tuong AuctionItem voi cac phu thuoc can thiet.
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
    // Ham tao: khoi tao doi tuong AuctionItem voi cac phu thuoc can thiet.
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
    // Ham tao: khoi tao doi tuong AuctionItem voi cac phu thuoc can thiet.
    public AuctionItem(
            int id,
            String name,
            String description,
            double startPrice,
            long startTime,
            long endTime,
            String category,
            String imageSource,
            int sellerId
    ) {
        this(id, name, description, startPrice, startTime, endTime, category, imageSource, null, sellerId);
    }
    // Ham tao: khoi tao doi tuong AuctionItem voi cac phu thuoc can thiet.
    public AuctionItem(int id, String name, String description, double startPrice, long startTime, long endTime, String category, int sellerId) {
        this(id, name, description, startPrice, startTime, endTime, category, null, sellerId);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get seller id.
    public int getSellerId() {
        return sellerId;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set seller id.
    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get winner id.
    public int getWinnerId() {
        return winnerId;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set winner id.
    public void setWinnerId(int winnerId) {
        this.winnerId = winnerId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get status.
    public AuctionStatus getStatus() {
        return status;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set status.
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get anti sniping extension count.
    public int getAntiSnipingExtensionCount() {
        return antiSnipingExtensionCount;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set anti sniping extension count.
    public void setAntiSnipingExtensionCount(int antiSnipingExtensionCount) {
        this.antiSnipingExtensionCount = antiSnipingExtensionCount;
    }

    @Override
    // Phuong thuc: thuc hien chuc nang to string trong lop AuctionItem.
    public String toString() {
        return id + "," + name + "," + description + "," + startPrice + "," + currentHighestBid + "," + startTime + "," + endTime + "," + category + "," + imageSource + "," + createdAt + "," + updatedAt + "," + sellerId + "," + winnerId + "," + status.name() + "," + antiSnipingExtensionCount;
    }
}
