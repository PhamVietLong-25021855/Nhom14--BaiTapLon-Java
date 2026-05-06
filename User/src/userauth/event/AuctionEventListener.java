package userauth.event;

// File note: Contract listener cho các thành phần muốn nghe sự kiện auction.
// Contract tá»‘i giáº£n cho cÃ¡c mÃ n hÃ¬nh muá»‘n nghe thay Ä‘á»•i cá»§a auction theo kiá»ƒu pub/sub.
@FunctionalInterface
public interface AuctionEventListener {
    void onAuctionEvent(AuctionEvent event);
}

