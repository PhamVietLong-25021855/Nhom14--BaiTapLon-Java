package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.support.TestDaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionServiceStatusAndSettlementTest {
    private TestDaos.InMemoryAuctionDao auctionDao;
    private AuctionService service;

    @BeforeEach
    void setUp() {
        auctionDao = new TestDaos.InMemoryAuctionDao();
        service = new AuctionService(auctionDao, new TestDaos.InMemoryAutoBidDao());
    }

    @Test
    void refreshAuctionStatusesStartsDueAuctionsAndFinishesExpiredAuctions() {
        long now = System.currentTimeMillis();
        auctionDao.saveAuction(auction(1, 7, now - 1000, now + 60_000, AuctionStatus.OPEN, -1));
        auctionDao.saveAuction(auction(2, 7, now - 60_000, now - 1000, AuctionStatus.RUNNING, 11));

        service.refreshAuctionStatuses();

        assertEquals(AuctionStatus.RUNNING, auctionDao.findAuctionById(1).getStatus());
        assertEquals(AuctionStatus.FINISHED, auctionDao.findAuctionById(2).getStatus());
    }

    @Test
    void markAuctionAsPaidRequiresOwnerFinishedStatusAndWinner() throws Exception {
        long now = System.currentTimeMillis();
        auctionDao.saveAuction(auction(1, 7, now - 60_000, now - 1000, AuctionStatus.FINISHED, -1));
        auctionDao.saveAuction(auction(2, 7, now - 60_000, now - 1000, AuctionStatus.FINISHED, 11));
        auctionDao.saveAuction(auction(3, 7, now - 1000, now + 60_000, AuctionStatus.RUNNING, 11));

        assertThrows(ValidationException.class, () -> service.markAuctionAsPaid(1, 7));
        assertThrows(UnauthorizedException.class, () -> service.markAuctionAsPaid(2, 8));
        assertThrows(ValidationException.class, () -> service.markAuctionAsPaid(3, 7));

        service.markAuctionAsPaid(2, 7);

        assertEquals(AuctionStatus.PAID, auctionDao.findAuctionById(2).getStatus());
    }

    @Test
    void cancelFinishedAuctionClearsWinner() throws Exception {
        long now = System.currentTimeMillis();
        auctionDao.saveAuction(auction(1, 7, now - 60_000, now - 1000, AuctionStatus.FINISHED, 11));

        service.cancelFinishedAuction(1, 7);

        assertEquals(AuctionStatus.CANCELED, auctionDao.findAuctionById(1).getStatus());
        assertEquals(-1, auctionDao.findAuctionById(1).getWinnerId());
    }

    private AuctionItem auction(int id, int sellerId, long startTime, long endTime, AuctionStatus status, int winnerId) {
        long now = System.currentTimeMillis();
        return new AuctionItem(
                id,
                "Laptop",
                "Desc",
                100.0,
                150.0,
                startTime,
                endTime,
                "Electronics",
                now,
                now,
                sellerId,
                winnerId,
                status
        );
    }
}
