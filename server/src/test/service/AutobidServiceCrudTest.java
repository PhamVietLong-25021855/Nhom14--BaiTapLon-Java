package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.dao.TestDaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutobidServiceCrudTest {
    private TestDaos.InMemoryAutoBidDao autoBidDao;
    private AutobidService service;

    @BeforeEach
    void setUp() {
        autoBidDao = new TestDaos.InMemoryAutoBidDao();
        service = new AutobidService(autoBidDao, null);
    }

    @Test
    void createAutobidPersistsConfiguration() throws Exception {
        service.createAutobid(10, 1, 500.0, 25.0);

        AutoBid saved = autoBidDao.findAutoBidByAuctionBidder(1, 10);

        assertNotNull(saved);
        assertEquals(500.0, saved.getMaxPrice());
        assertEquals(25.0, saved.getIncrement());
    }

    @Test
    void createAutobidTriggersRunningAuctionOnServerService() throws Exception {
        long now = System.currentTimeMillis();
        TestDaos.InMemoryAuctionDao auctionDao = new TestDaos.InMemoryAuctionDao();
        auctionDao.saveAuction(new AuctionItem(
                1,
                "Laptop",
                "Desc",
                100.0,
                100.0,
                now - 1000,
                now + 60_000,
                "Electronics",
                null,
                null,
                now,
                now,
                7,
                -1,
                AuctionStatus.RUNNING,
                0
        ));
        AuctionService auctionService = new AuctionService(auctionDao, autoBidDao);
        AutobidService serviceWithAuction = new AutobidService(autoBidDao, auctionService);

        serviceWithAuction.createAutobid(10, 1, 500.0, 25.0);

        assertEquals(125.0, auctionDao.findAuctionById(1).getCurrentHighestBid());
        assertEquals(10, auctionDao.findAuctionById(1).getWinnerId());
        assertEquals(1, auctionDao.findBidsByAuction(1).size());
    }

    @Test
    void createAutobidRejectsDuplicateForSameAuctionAndBidder() throws Exception {
        service.createAutobid(10, 1, 500.0, 25.0);

        assertThrows(ValidationException.class, () -> service.createAutobid(10, 1, 600.0, 30.0));
    }

    @Test
    void updateAutobidRequiresOwnerAndValidAmounts() throws Exception {
        service.createAutobid(10, 1, 500.0, 25.0);
        AutoBid saved = autoBidDao.findAutoBidByAuctionBidder(1, 10);

        assertThrows(UnauthorizedException.class, () -> service.updateAutobid(11, saved.getId(), 600.0, 30.0));
        assertThrows(ValidationException.class, () -> service.updateAutobid(10, saved.getId(), 600.0, 0.0));

        service.updateAutobid(10, saved.getId(), 600.0, 30.0);

        assertEquals(600.0, autoBidDao.findAutoBidById(saved.getId()).getMaxPrice());
        assertEquals(30.0, autoBidDao.findAutoBidById(saved.getId()).getIncrement());
    }

    @Test
    void deleteAutobidRequiresOwner() throws Exception {
        service.createAutobid(10, 1, 500.0, 25.0);
        AutoBid saved = autoBidDao.findAutoBidByAuctionBidder(1, 10);

        assertThrows(UnauthorizedException.class, () -> service.deleteAutobid(11, saved.getId()));

        service.deleteAutobid(10, saved.getId());

        assertNull(autoBidDao.findAutoBidById(saved.getId()));
    }
}
