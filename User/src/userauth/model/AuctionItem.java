package userauth.model;

public class AuctionItem extends Item {
    public static final int DEFAULT_EXTENSION_THRESHOLD_SECONDS = 30;
    public static final int DEFAULT_EXTENSION_DURATION_SECONDS = 60;
    public static final int DEFAULT_MAX_EXTENSION_COUNT = 5;

    private int sellerId;
    private int winnerId;
    private AuctionStatus status;
    private long originalEndTime;
    private int extensionCount;
    private int maxExtensionCount;
    private boolean antiSnipingEnabled;
    private int extensionThresholdSeconds;
    private int extensionDurationSeconds;

    public AuctionItem(
            int id,
            String name,
            String description,
            double startPrice,
            double currentHighestBid,
            long startTime,
            long endTime,
            long originalEndTime,
            int extensionCount,
            int maxExtensionCount,
            boolean antiSnipingEnabled,
            int extensionThresholdSeconds,
            int extensionDurationSeconds,
            String category,
            String imageSource,
            long createdAt,
            long updatedAt,
            int sellerId,
            int winnerId,
            AuctionStatus status
    ) {
        super(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, imageSource, createdAt, updatedAt);
        this.originalEndTime = originalEndTime;
        this.extensionCount = extensionCount;
        this.maxExtensionCount = maxExtensionCount;
        this.antiSnipingEnabled = antiSnipingEnabled;
        this.extensionThresholdSeconds = extensionThresholdSeconds;
        this.extensionDurationSeconds = extensionDurationSeconds;
        this.sellerId = sellerId;
        this.winnerId = winnerId;
        this.status = status;
    }

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
        this(
                id,
                name,
                description,
                startPrice,
                currentHighestBid,
                startTime,
                endTime,
                endTime,
                0,
                DEFAULT_MAX_EXTENSION_COUNT,
                true,
                DEFAULT_EXTENSION_THRESHOLD_SECONDS,
                DEFAULT_EXTENSION_DURATION_SECONDS,
                category,
                imageSource,
                createdAt,
                updatedAt,
                sellerId,
                winnerId,
                status
        );
    }

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
        this(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, null, createdAt, updatedAt, sellerId, winnerId, status);
    }

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
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                sellerId,
                -1,
                AuctionStatus.OPEN
        );
    }

    public AuctionItem(int id, String name, String description, double startPrice, long startTime, long endTime, String category, int sellerId) {
        this(id, name, description, startPrice, startTime, endTime, category, null, sellerId);
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public int getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(int winnerId) {
        this.winnerId = winnerId;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public long getOriginalEndTime() {
        return originalEndTime;
    }

    public void setOriginalEndTime(long originalEndTime) {
        this.originalEndTime = originalEndTime;
    }

    public int getExtensionCount() {
        return extensionCount;
    }

    public void setExtensionCount(int extensionCount) {
        this.extensionCount = extensionCount;
    }

    public int getMaxExtensionCount() {
        return maxExtensionCount;
    }

    public void setMaxExtensionCount(int maxExtensionCount) {
        this.maxExtensionCount = maxExtensionCount;
    }

    public boolean isAntiSnipingEnabled() {
        return antiSnipingEnabled;
    }

    public void setAntiSnipingEnabled(boolean antiSnipingEnabled) {
        this.antiSnipingEnabled = antiSnipingEnabled;
    }

    public int getExtensionThresholdSeconds() {
        return extensionThresholdSeconds;
    }

    public void setExtensionThresholdSeconds(int extensionThresholdSeconds) {
        this.extensionThresholdSeconds = extensionThresholdSeconds;
    }

    public int getExtensionDurationSeconds() {
        return extensionDurationSeconds;
    }

    public void setExtensionDurationSeconds(int extensionDurationSeconds) {
        this.extensionDurationSeconds = extensionDurationSeconds;
    }

    @Override
    public String toString() {
        return id + "," + name + "," + description + "," + startPrice + "," + currentHighestBid + "," + startTime + ","
                + endTime + "," + originalEndTime + "," + extensionCount + "," + maxExtensionCount + ","
                + antiSnipingEnabled + "," + extensionThresholdSeconds + "," + extensionDurationSeconds + ","
                + category + "," + imageSource + "," + createdAt + "," + updatedAt + "," + sellerId + ","
                + winnerId + "," + status.name();
    }
}
