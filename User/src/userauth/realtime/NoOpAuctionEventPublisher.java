package userauth.realtime;

public class NoOpAuctionEventPublisher implements AuctionEventPublisher {
    @Override
    public void publish(AuctionUpdateEvent event) {
        // No-op. Dùng khi dự án chưa gắn Socket/Observer thật.
    }
}
