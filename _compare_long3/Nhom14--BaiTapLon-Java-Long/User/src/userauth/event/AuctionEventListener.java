package userauth.event;

@FunctionalInterface
public interface AuctionEventListener {
    void onAuctionEvent(AuctionEvent event);
}
