package userauth.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// File note: Event bus nội bộ cho phép publish/subscribe thay đổi auction giữa các màn hình.
// Event bus ná»™i bá»™ giÃºp cÃ¡c mÃ n hÃ¬nh refresh theo sá»± kiá»‡n mÃ  khÃ´ng pháº£i phá»¥ thuá»™c trá»±c tiáº¿p vÃ o nhau.
public final class AuctionEventBus {
    private static final AuctionEventBus INSTANCE = new AuctionEventBus();

    // Danh sÃ¡ch listener an toÃ n cho trÆ°á»ng há»£p subscribe/unsubscribe khi app Ä‘ang cháº¡y.
    private final List<AuctionEventListener> listeners = new CopyOnWriteArrayList<>();

    private AuctionEventBus() {
    }

    public static AuctionEventBus getInstance() {
        return INSTANCE;
    }

    // ÄÄƒng kÃ½ mÃ n hÃ¬nh hoáº·c service muá»‘n nháº­n thÃ´ng bÃ¡o.
    public void subscribe(AuctionEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    // Gá»¡ listener khi mÃ n hÃ¬nh bá»‹ deactivate Ä‘á»ƒ trÃ¡nh leak.
    public void unsubscribe(AuctionEventListener listener) {
        listeners.remove(listener);
    }

    // PhÃ¡t sá»± kiá»‡n hiá»‡n táº¡i cho toÃ n bá»™ listener Ä‘Ã£ Ä‘Äƒng kÃ½.
    public void publish(AuctionEvent event) {
        if (event == null) {
            return;
        }
        for (AuctionEventListener listener : listeners) {
            listener.onAuctionEvent(event);
        }
    }
}

