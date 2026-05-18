package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.common.AuctionRules;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;
import userauth.dao.TestDaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionServiceLifecycleTest {
    private TestDaos.InMemoryAuctionDao auctionDao;
    private AuctionService service;

    @BeforeEach
    void setUp() {
        auctionDao = new TestDaos.InMemoryAuctionDao();
        service = new AuctionService(auctionDao, new TestDaos.InMemoryAutoBidDao());
    }

    @Test
    void createAuctionSavesOpenAuctionAndNormalizesBlankImageSource() throws Exception {
        long now = System.currentTimeMillis();

        service.createAuction("Camera", "Desc", 100.0, now + 1000, now + 60_000, "Electronics", "  ", null, 7);

        AuctionItem saved = auctionDao.findAllAuctions().get(0);
        assertEquals(AuctionStatus.OPEN, saved.getStatus());
        assertEquals(100.0, saved.getCurrentHighestBid());
        assertNull(saved.getImageSource());
    }

    @Test
    void createAuctionPersistsAllSubmittedProductFields() throws Exception {
        long start = System.currentTimeMillis() + 1000;
        long end = start + 60_000;
        byte[] imageData = new byte[]{1, 3, 5, 7};

        service.createAuction(
                "Laptop Pro",
                "Clean condition",
                1200.0,
                start,
                end,
                "Electronics",
                "laptop.png",
                imageData,
                7
        );

        AuctionItem saved = auctionDao.findAllAuctions().get(0);
        assertEquals("Laptop Pro", saved.getName());
        assertEquals("Clean condition", saved.getDescription());
        assertEquals(1200.0, saved.getStartPrice());
        assertEquals(1200.0, saved.getCurrentHighestBid());
        assertEquals(start, saved.getStartTime());
        assertEquals(end, saved.getEndTime());
        assertEquals("Electronics", saved.getCategory());
        assertEquals("laptop.png", saved.getImageSource());
        org.junit.jupiter.api.Assertions.assertArrayEquals(imageData, saved.getImageData());
        assertEquals(7, saved.getSellerId());
        assertEquals(-1, saved.getWinnerId());
        assertEquals(AuctionStatus.OPEN, saved.getStatus());
        assertEquals(0, saved.getAntiSnipingExtensionCount());
    }

    @Test
    void createAuctionRejectsOversizedImage() {
        long now = System.currentTimeMillis();
        byte[] image = new byte[AuctionRules.MAX_IMAGE_BYTES + 1];

        assertThrows(ValidationException.class,
                () -> service.createAuction("Camera", "Desc", 100.0, now + 1000, now + 60_000,
                        "Electronics", null, image, 7));
    }

    @Test
    void updateAuctionRejectsNonOwnerAndExistingBids() {
        long now = System.currentTimeMillis();
        AuctionItem item = openAuction(1, 7, now + 1000, now + 60_000);
        auctionDao.saveAuction(item);

        assertThrows(UnauthorizedException.class,
                () -> service.updateAuction(1, 8, "Updated", "Desc", 120.0, now + 2000, now + 70_000,
                        "Collectibles", null, null));

        auctionDao.saveBid(new BidTransaction(0, 1, 10, 130.0, now, "ACCEPTED"));

        assertThrows(ValidationException.class,
                () -> service.updateAuction(1, 7, "Updated", "Desc", 120.0, now + 2000, now + 70_000,
                        "Collectibles", null, null));
    }

    @Test
    void deleteAuctionWithoutBidsRemovesIt() throws Exception {
        long now = System.currentTimeMillis();
        auctionDao.saveAuction(openAuction(1, 7, now + 1000, now + 60_000));

        service.deleteAuction(1, 7);

        assertNull(auctionDao.findAuctionById(1));
    }

    @Test
    void deleteAuctionWithBidsCancelsInsteadOfRemovingHistory() throws Exception {
        long now = System.currentTimeMillis();
        auctionDao.saveAuction(openAuction(1, 7, now - 1000, now + 60_000));
        auctionDao.saveBid(new BidTransaction(0, 1, 10, 130.0, now, "ACCEPTED"));

        service.deleteAuction(1, 7);

        assertEquals(AuctionStatus.CANCELED, auctionDao.findAuctionById(1).getStatus());
        assertEquals(1, auctionDao.findBidsByAuction(1).size());
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
}
