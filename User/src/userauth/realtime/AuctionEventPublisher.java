package userauth.realtime;

public interface AuctionEventPublisher {
    void publish(AuctionUpdateEvent event);
}
