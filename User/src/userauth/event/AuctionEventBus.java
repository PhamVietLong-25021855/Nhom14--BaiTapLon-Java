package userauth.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AuctionEventBus {
    private static final AuctionEventBus INSTANCE = new AuctionEventBus();

    private final List<AuctionEventListener> listeners = new CopyOnWriteArrayList<>();

    private AuctionEventBus() {
    }

    public static AuctionEventBus getInstance() {
        return INSTANCE;
    }

    public void subscribe(AuctionEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(AuctionEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(AuctionEvent event) {
        if (event == null) {
            return;
        }
        for (AuctionEventListener listener : listeners) {
            listener.onAuctionEvent(event);
        }
    }
}
