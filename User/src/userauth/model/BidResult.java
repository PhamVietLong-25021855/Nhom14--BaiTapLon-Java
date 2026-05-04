package userauth.model;

import java.io.Serial;
import java.io.Serializable;

public class BidResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean accepted;
    private final long bidId;
    private final int auctionId;
    private final int bidderId;
    private final double acceptedAmount;
    private final double currentHighestBid;
    private final int currentWinnerId;
    private final long previousEndTime;
    private final long auctionEndTime;
    private final boolean auctionExtended;
    private final int extensionCount;
    private final AuctionStatus auctionStatus;
    private final String message;

    public BidResult(boolean accepted, long bidId, int auctionId, int bidderId, double acceptedAmount,
                     double currentHighestBid, int currentWinnerId, long previousEndTime, long auctionEndTime,
                     boolean auctionExtended, int extensionCount, AuctionStatus auctionStatus, String message) {
        this.accepted = accepted;
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.acceptedAmount = acceptedAmount;
        this.currentHighestBid = currentHighestBid;
        this.currentWinnerId = currentWinnerId;
        this.previousEndTime = previousEndTime;
        this.auctionEndTime = auctionEndTime;
        this.auctionExtended = auctionExtended;
        this.extensionCount = extensionCount;
        this.auctionStatus = auctionStatus;
        this.message = message;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public long getBidId() {
        return bidId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public double getAcceptedAmount() {
        return acceptedAmount;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public int getCurrentWinnerId() {
        return currentWinnerId;
    }

    public long getPreviousEndTime() {
        return previousEndTime;
    }

    public long getAuctionEndTime() {
        return auctionEndTime;
    }

    public boolean isAuctionExtended() {
        return auctionExtended;
    }

    public int getExtensionCount() {
        return extensionCount;
    }

    public AuctionStatus getAuctionStatus() {
        return auctionStatus;
    }

    public String getMessage() {
        return message;
    }
}
