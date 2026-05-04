package userauth.model;

public class AuctionItem extends Item {
    private int sellerId;
    private int winnerId;
    private AuctionStatus status;

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
        super(id, name, description, startPrice, currentHighestBid, startTime, endTime, category, imageSource, createdAt, updatedAt);
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

    @Override
    public String toString() {
        return id + "," + name + "," + description + "," + startPrice + "," + currentHighestBid + "," + startTime + "," + endTime + "," + category + "," + imageSource + "," + createdAt + "," + updatedAt + "," + sellerId + "," + winnerId + "," + status.name();
    }
}
