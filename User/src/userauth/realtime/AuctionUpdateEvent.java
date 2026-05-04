package userauth.realtime;

import java.io.Serial;
import java.io.Serializable;

public class AuctionUpdateEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final AuctionEventType eventType;
    private final int auctionId;
    private final long bidId;
    private final double newHighestBid;
    private final int currentWinnerId;
    private final long previousEndTime;
    private final long newEndTime;
    private final int extensionCount;

    public AuctionUpdateEvent(AuctionEventType eventType, int auctionId, long bidId, double newHighestBid,
                              int currentWinnerId, long previousEndTime, long newEndTime, int extensionCount) {
        this.eventType = eventType;
        this.auctionId = auctionId;
        this.bidId = bidId;
        this.newHighestBid = newHighestBid;
        this.currentWinnerId = currentWinnerId;
        this.previousEndTime = previousEndTime;
        this.newEndTime = newEndTime;
        this.extensionCount = extensionCount;
    }

    public AuctionEventType getEventType() {
        return eventType;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public long getBidId() {
        return bidId;
    }

    public double getNewHighestBid() {
        return newHighestBid;
    }

    public int getCurrentWinnerId() {
        return currentWinnerId;
    }

    public long getPreviousEndTime() {
        return previousEndTime;
    }

    public long getNewEndTime() {
        return newEndTime;
    }

    public int getExtensionCount() {
        return extensionCount;
    }
}
