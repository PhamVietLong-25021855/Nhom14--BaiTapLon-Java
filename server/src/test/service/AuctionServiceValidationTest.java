package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.exception.AuctionClosedException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.dao.TestDaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionServiceValidationTest {
    private TestDaos.InMemoryAuctionDao auctionDao;
    private AuctionService service;

    @BeforeEach
    void setUp() {
        auctionDao = new TestDaos.InMemoryAuctionDao();
        service = new AuctionService(auctionDao, new TestDaos.InMemoryAutoBidDao());
    }

    @Test
    void createAuctionRejectsInvalidCoreFieldsWithoutSavingAnything() {
        long now = System.currentTimeMillis();

        assertThrows(ValidationException.class,
                () -> service.createAuction(" ", "Desc", 100.0, now + 1000, now + 60_000,
                        "Electronics", null, null, 7));
        assertThrows(ValidationException.class,
                () -> service.createAuction("Camera", "Desc", 0.0, now + 1000, now + 60_000,
                        "Electronics", null, null, 7));
        assertThrows(ValidationException.class,
                () -> service.createAuction("Camera", "Desc", 100.0, now + 60_000, now + 1000,
                        "Electronics", null, null, 7));
        assertThrows(ValidationException.class,
                () -> service.createAuction("Camera", "Desc", 100.0, now - 60_000, now - 1000,
                        "Electronics", null, null, 7));

        assertTrue(auctionDao.findAllAuctions().isEmpty());
    }

    @Test
    void updateAuctionPersistsNewFieldsAndResetsDerivedBidState() throws Exception {
        long now = System.currentTimeMillis();
        auctionDao.saveAuction(openAuction(1, 7, now + 1000, now + 60_000));
        auctionDao.findAuctionById(1).setAntiSnipingExtensionCount(2);

        service.updateAuction(
                1,
                7,
                "Updated Camera",
                "Updated description",
                250.0,
                now + 5000,
                now + 120_000,
                "Collectibles",
                "  camera.png  ",
                new byte[]{1, 2, 3}
        );

        AuctionItem updated = auctionDao.findAuctionById(1);
        assertEquals("Updated Camera", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(250.0, updated.getStartPrice());
        assertEquals(250.0, updated.getCurrentHighestBid());
        assertEquals("Collectibles", updated.getCategory());
        assertEquals("camera.png", updated.getImageSource());
        assertEquals(0, updated.getAntiSnipingExtensionCount());
        assertEquals(AuctionStatus.OPEN, updated.getStatus());
        assertTrue(auctionDao.findBidsByAuction(1).isEmpty());
    }

    @Test
    void placeBidRejectsMissingNotRunningAndOutsideTimeAuctions() {
        long now = System.currentTimeMillis();
        auctionDao.saveAuction(openAuction(1, 7, now + 1000, now + 60_000));
        auctionDao.saveAuction(runningAuction(2, now + 1000, now + 60_000));
        auctionDao.saveAuction(runningAuction(3, now - 60_000, now - 1000));

        assertThrows(ItemNotFoundException.class, () -> service.placeBid(99, 10, 150.0));
        assertThrows(AuctionClosedException.class, () -> service.placeBid(1, 10, 150.0));
        assertThrows(AuctionClosedException.class, () -> service.placeBid(2, 10, 150.0));
        assertThrows(AuctionClosedException.class, () -> service.placeBid(3, 10, 150.0));

        assertTrue(auctionDao.findAllBids().isEmpty());
    }

    private AuctionItem openAuction(int id, int sellerId, long startTime, long endTime) {
        long now = System.currentTimeMillis();
        return new AuctionItem(
                id,
                "Camera",
                "Desc",
                100.0,
                100.0,
                startTime,
                endTime,
                "Electronics",
                now,
                now,
                sellerId,
                -1,
                AuctionStatus.OPEN
        );
    }

    private AuctionItem runningAuction(int id, long startTime, long endTime) {
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
                now,
                now,
                7,
                -1,
                AuctionStatus.RUNNING
        );
    }
}
