package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.support.TestDaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionServiceAutoBidTest {
    private TestDaos.InMemoryAuctionDao auctionDao;
    private TestDaos.InMemoryAutoBidDao autoBidDao;
    private AuctionService service;

    @BeforeEach
    void setUp() {
        auctionDao = new TestDaos.InMemoryAuctionDao();
        autoBidDao = new TestDaos.InMemoryAutoBidDao();
        service = new AuctionService(auctionDao, autoBidDao);
    }

    @Test
    void autoBidRespondsToManualBidAndBecomesWinner() throws Exception {
        long now = System.currentTimeMillis();
        auctionDao.saveAuction(runningAuction(1, now - 1000, now + 60_000, 0));
        autoBidDao.saveAutoBid(new AutoBid(0, 1, 20, 160.0, 20.0, now, now));

        service.placeBid(1, 10, 120.0);

        AuctionItem updated = auctionDao.findAuctionById(1);
        assertEquals(140.0, updated.getCurrentHighestBid());
        assertEquals(20, updated.getWinnerId());
        assertEquals(2, auctionDao.findBidsByAuction(1).size());
    }

    @Test
    void bidNearEndExtendsAuctionOnceForAntiSniping() throws Exception {
        long now = System.currentTimeMillis();
        long originalEndTime = now + 10_000;
        auctionDao.saveAuction(runningAuction(1, now - 1000, originalEndTime, 0));

        service.placeBid(1, 10, 120.0);

        AuctionItem updated = auctionDao.findAuctionById(1);
        assertTrue(updated.getEndTime() > originalEndTime);
        assertEquals(1, updated.getAntiSnipingExtensionCount());
    }

    @Test
    void antiSnipingDoesNotExtendAfterMaximumExtensions() throws Exception {
        long now = System.currentTimeMillis();
        long originalEndTime = now + 10_000;
        auctionDao.saveAuction(runningAuction(1, now - 1000, originalEndTime, AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS));

        service.placeBid(1, 10, 120.0);

        AuctionItem updated = auctionDao.findAuctionById(1);
        assertEquals(originalEndTime, updated.getEndTime());
        assertEquals(AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS, updated.getAntiSnipingExtensionCount());
    }

    private AuctionItem runningAuction(int id, long startTime, long endTime, int extensionCount) {
        long now = System.currentTimeMillis();
        return new AuctionItem(
                id,
                "Laptop",
                "Desc",
                100.0,
                100.0,
                startTime,
                endTime,
                "Electronics",
                null,
                null,
                now,
                now,
                7,
                -1,
                AuctionStatus.RUNNING,
                extensionCount
        );
    }
}
