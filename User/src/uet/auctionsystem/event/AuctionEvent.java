package uet.auctionsystem.event;

import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.AuctionStatus;

// Ghi chu file: File ho tro co che su kien; dung de phat va nhan cap nhat trang thai dau gia.
// Khai bao record AuctionEvent; phuc vu co che observer cho cac cap nhat dau gia.
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
    // PhÃ¢n loáº¡i event Ä‘á»ƒ UI cÃ³ thá»ƒ pháº£n á»©ng khÃ¡c nhau náº¿u cáº§n.
    public enum AuctionEventType {
        BID_ACTIVITY,
        ANTI_SNIPING_EXTENDED,
        STATUS_CHANGED,
        SETTLED
    }

    // Event cho trÆ°á»ng há»£p giÃ¡ hoáº·c ngÆ°á»i dáº«n Ä‘áº§u vá»«a thay Ä‘á»•i.
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac bid activity.
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

    // Event cho trÆ°á»ng há»£p bá»‹ gia háº¡n vÃ¬ anti-sniping.
    // Phuong thuc: thuc hien chuc nang anti sniping extended trong lop AuctionEvent.
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

    // Event cho cÃ¡c thay Ä‘á»•i tráº¡ng thÃ¡i nhÆ° OPEN -> RUNNING hoáº·c Ä‘Ã³ng tay.
    // Phuong thuc: thuc hien chuc nang status changed trong lop AuctionEvent.
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

    // Event cho cÃ¡c bÆ°á»›c settlement nhÆ° PAID hoáº·c CANCEL RESULT.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac settled.
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
