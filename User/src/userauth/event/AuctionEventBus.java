package userauth.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Ghi chu file: File ho tro co che su kien; dung de phat va nhan cap nhat trang thai dau gia.
// Khai bao lop AuctionEventBus; phuc vu co che observer cho cac cap nhat dau gia.
public final class AuctionEventBus {
    private static final AuctionEventBus INSTANCE = new AuctionEventBus();

    // Danh sÃ¡ch listener an toÃ n cho trÆ°á»ng há»£p subscribe/unsubscribe khi app Ä‘ang cháº¡y.
    private final List<AuctionEventListener> listeners = new CopyOnWriteArrayList<>();
    // Ham tao: khoi tao doi tuong AuctionEventBus voi cac phu thuoc can thiet.
    private AuctionEventBus() {
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get instance.
    public static AuctionEventBus getInstance() {
        return INSTANCE;
    }

    // ÄÄƒng kÃ½ mÃ n hÃ¬nh hoáº·c service muá»‘n nháº­n thÃ´ng bÃ¡o.
    // Phuong thuc: thuc hien chuc nang subscribe trong lop AuctionEventBus.
    public void subscribe(AuctionEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    // Gá»¡ listener khi mÃ n hÃ¬nh bá»‹ deactivate Ä‘á»ƒ trÃ¡nh leak.
    // Phuong thuc: thuc hien chuc nang unsubscribe trong lop AuctionEventBus.
    public void unsubscribe(AuctionEventListener listener) {
        listeners.remove(listener);
    }

    // PhÃ¡t sá»± kiá»‡n hiá»‡n táº¡i cho toÃ n bá»™ listener Ä‘Ã£ Ä‘Äƒng kÃ½.
    // Phuong thuc: thuc hien chuc nang publish trong lop AuctionEventBus.
    public void publish(AuctionEvent event) {
        if (event == null) {
            return;
        }
        for (AuctionEventListener listener : listeners) {
            listener.onAuctionEvent(event);
        }
    }
}
