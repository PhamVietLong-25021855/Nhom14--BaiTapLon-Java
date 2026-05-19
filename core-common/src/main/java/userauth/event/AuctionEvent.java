package userauth.event;

import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

public record AuctionEvent(
        int auctionId,
        AuctionEventType type,
        String summary,
        long occurredAt,
        AuctionStatus status,
        double currentHighestBid,
        int winnerId,
        long endTime
) {
    public enum AuctionEventType {
        BID_ACTIVITY,
        ANTI_SNIPING_EXTENDED,
        STATUS_CHANGED,
        SETTLED
    }

    public static AuctionEvent bidActivity(AuctionItem item, long occurredAt) {
        return new AuctionEvent(
                item.getId(),
                AuctionEventType.BID_ACTIVITY,
                "Bid activity updated.",
                occurredAt,
                item.getStatus(),
                item.getCurrentHighestBid(),
                item.getWinnerId(),
                item.getEndTime()
        );
    }

    public static AuctionEvent antiSnipingExtended(AuctionItem item, long occurredAt) {
        return new AuctionEvent(
                item.getId(),
                AuctionEventType.ANTI_SNIPING_EXTENDED,
                "Anti-sniping extended the closing time.",
                occurredAt,
                item.getStatus(),
                item.getCurrentHighestBid(),
                item.getWinnerId(),
                item.getEndTime()
        );
    }

    public static AuctionEvent statusChanged(AuctionItem item, long occurredAt, String summary) {
        return new AuctionEvent(
                item.getId(),
                AuctionEventType.STATUS_CHANGED,
                summary,
                occurredAt,
                item.getStatus(),
                item.getCurrentHighestBid(),
                item.getWinnerId(),
                item.getEndTime()
        );
    }

    public static AuctionEvent settled(AuctionItem item, long occurredAt, String summary) {
        return new AuctionEvent(
                item.getId(),
                AuctionEventType.SETTLED,
                summary,
                occurredAt,
                item.getStatus(),
                item.getCurrentHighestBid(),
                item.getWinnerId(),
                item.getEndTime()
        );
    }
}
