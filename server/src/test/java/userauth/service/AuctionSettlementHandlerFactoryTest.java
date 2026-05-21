package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionSettlementHandlerFactoryTest {
    private final AuctionSettlementHandlerFactory factory = new AuctionSettlementHandlerFactory();

    @Test
    void paidHandlerRequiresWinnerAndUpdatesStatus() {
        AuctionItem item = finishedAuction();
        item.setWinnerId(42);
        AuctionSettlementHandler handler = factory.create(AuctionStatus.PAID);

        assertEquals(AuctionStatus.PAID, handler.targetStatus());
        assertDoesNotThrow(() -> handler.validate(item));

        handler.apply(item, 12_345L);

        assertEquals(AuctionStatus.PAID, item.getStatus());
        assertEquals(42, item.getWinnerId());
        assertEquals(12_345L, item.getUpdatedAt());
        assertEquals("Auction marked as paid.", handler.summary(item));
    }

    @Test
    void paidHandlerRejectsAuctionWithoutWinner() {
        AuctionItem item = finishedAuction();
        AuctionSettlementHandler handler = factory.create(AuctionStatus.PAID);

        assertThrows(ValidationException.class, () -> handler.validate(item));
    }

    @Test
    void canceledHandlerClearsWinnerAndUpdatesStatus() {
        AuctionItem item = finishedAuction();
        item.setWinnerId(42);
        AuctionSettlementHandler handler = factory.create(AuctionStatus.CANCELED);

        assertEquals(AuctionStatus.CANCELED, handler.targetStatus());
        assertDoesNotThrow(() -> handler.validate(item));

        handler.apply(item, 12_345L);

        assertEquals(AuctionStatus.CANCELED, item.getStatus());
        assertEquals(-1, item.getWinnerId());
        assertEquals(12_345L, item.getUpdatedAt());
        assertEquals("Auction result has been cancelled.", handler.summary(item));
    }

    @Test
    void unsupportedStatusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> factory.create(AuctionStatus.FINISHED));
    }

    private static AuctionItem finishedAuction() {
        return new AuctionItem(
                10,
                "Laptop",
                "Test auction",
                1_000_000.0,
                1_500_000.0,
                1_000L,
                2_000L,
                "Electronics",
                900L,
                1_100L,
                5,
                -1,
                AuctionStatus.FINISHED
        );
    }
}
