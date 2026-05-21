package userauth.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import userauth.model.AuctionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AuctionEventBusTest {
    private final AuctionEventBus eventBus = AuctionEventBus.getInstance();
    private final List<AuctionEventListener> listeners = new ArrayList<>();

    @AfterEach
    void unsubscribeListeners() {
        for (AuctionEventListener listener : listeners) {
            eventBus.unsubscribe(listener);
        }
        listeners.clear();
    }

    @Test
    void publishDispatchesEventToSubscribedListener() {
        AtomicReference<AuctionEvent> receivedEvent = new AtomicReference<>();
        subscribe(receivedEvent::set);
        AuctionEvent event = sampleEvent();

        eventBus.publish(event);

        assertSame(event, receivedEvent.get());
    }

    @Test
    void unsubscribeStopsFutureDelivery() {
        AtomicInteger deliveryCount = new AtomicInteger();
        AuctionEventListener listener = ignored -> deliveryCount.incrementAndGet();
        subscribe(listener);

        eventBus.publish(sampleEvent());
        eventBus.unsubscribe(listener);
        listeners.remove(listener);
        eventBus.publish(sampleEvent());

        assertEquals(1, deliveryCount.get());
    }

    @Test
    void nullListenerAndNullEventAreIgnored() {
        AuctionEventListener listener = ignored -> {
            throw new AssertionError("Null events should not be delivered.");
        };
        subscribe(listener);

        assertDoesNotThrow(() -> eventBus.subscribe(null));
        assertDoesNotThrow(() -> eventBus.publish(null));
    }

    private void subscribe(AuctionEventListener listener) {
        eventBus.subscribe(listener);
        listeners.add(listener);
    }

    private static AuctionEvent sampleEvent() {
        return new AuctionEvent(
                7,
                AuctionEvent.AuctionEventType.STATUS_CHANGED,
                "Auction status changed.",
                1_000L,
                AuctionStatus.RUNNING,
                250_000.0,
                3,
                2_000L
        );
    }
}
