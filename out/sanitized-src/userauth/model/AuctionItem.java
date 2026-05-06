package userauth.model;

// File note: Model phiên đấu giá; chứa dữ liệu seller, winner, trạng thái, ảnh và anti-sniping.
// Model chÃ­nh cá»§a má»™t phiÃªn Ä‘áº¥u giÃ¡; má»Ÿ rá»™ng Item báº±ng thÃ´ng tin seller/winner/tráº¡ng thÃ¡i.
public class AuctionItem extends Item {
    private int sellerId;
    private int winnerId;
    private AuctionStatus status;
    // Äáº¿m sá»‘ láº§n Ä‘Ã£ Ä‘Æ°á»£c gia háº¡n á»Ÿ cuá»‘i phiÃªn vÃ¬ anti-sniping.
    private int antiSnipingExtensionCount;

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

    public int getAntiSnipingExtensionCount() {
        return antiSnipingExtensionCount;
    }

    public void setAntiSnipingExtensionCount(int antiSnipingExtensionCount) {
        this.antiSnipingExtensionCount = antiSnipingExtensionCount;
    }

    @Override
    public String toString() {
        return id + "," + name + "," + description + "," + startPrice + "," + currentHighestBid + "," + startTime + "," + endTime + "," + category + "," + imageSource + "," + createdAt + "," + updatedAt + "," + sellerId + "," + winnerId + "," + status.name() + "," + antiSnipingExtensionCount;
    }
}

