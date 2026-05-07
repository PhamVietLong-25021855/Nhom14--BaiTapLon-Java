package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;
import userauth.support.TestDaos;

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
        service = new AutobidService(autoBidDao);
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
