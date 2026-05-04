package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiSnipingServiceTest {

    private final AntiSnipingService antiSnipingService = new AntiSnipingService();

    @Test
    void shouldNotExtendWhenBidIsOutsideThreshold() {
        long now = System.currentTimeMillis();
        AuctionItem auction = createAuction(now + 120_000);

        assertFalse(antiSnipingService.shouldExtend(auction, now));
    }

    @Test
    void shouldExtendWhenBidIsInsideThreshold() {
        long now = System.currentTimeMillis();
        AuctionItem auction = createAuction(now + 10_000);

        assertTrue(antiSnipingService.shouldExtend(auction, now));
    }

    @Test
    void shouldNotExtendWhenMaxExtensionCountReached() {
        long now = System.currentTimeMillis();
        AuctionItem auction = createAuction(now + 10_000);
        auction.setMaxExtensionCount(2);
        auction.setExtensionCount(2);

        assertFalse(antiSnipingService.shouldExtend(auction, now));
    }

    @Test
    void extendAuctionUpdatesEndTimeAndCount() {
        long now = System.currentTimeMillis();
        long originalEndTime = now + 10_000;
        AuctionItem auction = createAuction(originalEndTime);

        boolean extended = antiSnipingService.extendAuction(auction, now);

        assertTrue(extended);
        assertEquals(originalEndTime + 60_000, auction.getEndTime());
        assertEquals(1, auction.getExtensionCount());
        assertEquals(originalEndTime, auction.getOriginalEndTime());
    }

    private static AuctionItem createAuction(long endTime) {
        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Console",
                "Gaming console",
                100_000,
                100_000,
                now - 60_000,
                endTime,
                "Electronics",
                null,
                now,
                now,
                88,
                -1,
                AuctionStatus.RUNNING
        );
        auction.setAntiSnipingEnabled(true);
        auction.setOriginalEndTime(endTime);
        auction.setExtensionThresholdSeconds(30);
        auction.setExtensionDurationSeconds(60);
        auction.setMaxExtensionCount(5);
        auction.setExtensionCount(0);
        return auction;
    }
}
