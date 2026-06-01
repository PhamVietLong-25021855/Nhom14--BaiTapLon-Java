package userauth.model;

public class AuctionItem extends Item {
    private double bidStep;
    private int sellerId;
    private int winnerId;
    private AuctionStatus status;
    private int antiSnipingExtensionCount;

    public AuctionItem(int id, String name, String description, double startPrice, double currentHighestBid,
                       long startTime, long endTime, String category, String imageSource, byte[] imageData,
                       long createdAt, long updatedAt, double bidStep, int sellerId, int winnerId, AuctionStatus status,
                       int antiSnipingExtensionCount) {
        super(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, imageSource, imageData, createdAt, updatedAt);
        this.bidStep = bidStep;
        this.sellerId = sellerId;
        this.winnerId = winnerId;
        this.status = status;
        this.antiSnipingExtensionCount = antiSnipingExtensionCount;
    }

    public AuctionItem(int id, String name, String description, double startPrice, double currentHighestBid,
                       long startTime, long endTime, String category, String imageSource, byte[] imageData,
                       long createdAt, long updatedAt, int sellerId, int winnerId, AuctionStatus status,
                       int antiSnipingExtensionCount) {
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, imageSource, imageData,
                createdAt, updatedAt, 0.0, sellerId, winnerId, status, antiSnipingExtensionCount);
    }

    public AuctionItem(int id, String name, String description, double startPrice, double currentHighestBid,
                       long startTime, long endTime, String category, String imageSource, byte[] imageData,
                       long createdAt, long updatedAt, int sellerId, int winnerId, AuctionStatus status) {
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, imageSource, imageData, createdAt, updatedAt, 0.0, sellerId, winnerId, status, 0);
    }

    public AuctionItem(int id, String name, String description, double startPrice, double currentHighestBid,
                       long startTime, long endTime, String category, long createdAt, long updatedAt,
                       int sellerId, int winnerId, AuctionStatus status) {
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, null, null, createdAt, updatedAt, 0.0, sellerId, winnerId, status, 0);
    }

    public AuctionItem(int id, String name, String description, double startPrice, long startTime, long endTime,
                       String category, String imageSource, byte[] imageData, double bidStep, int sellerId) {
        this(id, name, description, startPrice, startPrice, startTime, endTime, category, imageSource, imageData,
                System.currentTimeMillis(), System.currentTimeMillis(), bidStep, sellerId, -1, AuctionStatus.OPEN, 0);
    }

    public AuctionItem(int id, String name, String description, double startPrice, long startTime, long endTime,
                       String category, String imageSource, byte[] imageData, int sellerId) {
        this(id, name, description, startPrice, startTime, endTime, category, imageSource, imageData, 0.0, sellerId);
    }

    public AuctionItem(int id, String name, String description, double startPrice, long startTime, long endTime,
                       String category, int sellerId) {
        this(id, name, description, startPrice, startTime, endTime, category, null, null, sellerId);
    }

    public double getBidStep() { return bidStep; }
    public void setBidStep(double bidStep) { this.bidStep = bidStep; }
    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public int getWinnerId() { return winnerId; }
    public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public int getAntiSnipingExtensionCount() { return antiSnipingExtensionCount; }
    public void setAntiSnipingExtensionCount(int count) { this.antiSnipingExtensionCount = count; }

    @Override
    public String toString() {
        return id + "," + name + "," + description + "," + startPrice + "," + currentHighestBid + "," + startTime + "," + endTime + "," + category + "," + imageSource + "," + createdAt + "," + updatedAt + "," + bidStep + "," + sellerId + "," + winnerId + "," + (status != null ? status.name() : "null") + "," + antiSnipingExtensionCount;
    }
}
