package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.dao.AutoBidDAO;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutobidServiceTest {

    @Test
    void createAutobidRejectsIncrementGreaterThanMaxPrice() {
        InMemoryAutoBidDAO autoBidDAO = new InMemoryAutoBidDAO();
        AutobidService service = new AutobidService(autoBidDAO, null);

        assertThrows(ValidationException.class, () -> service.createAutobid(5, 10, 100.0, 120.0));
    }

    @Test
    void updateAutobidRejectsIncrementGreaterThanMaxPrice() throws Exception {
        InMemoryAutoBidDAO autoBidDAO = new InMemoryAutoBidDAO();
        AutoBid autoBid = new AutoBid(1, 10, 5, 200.0, 20.0, 1_000L, 1_000L);
        autoBidDAO.autoBids.add(autoBid);
        AutobidService service = new AutobidService(autoBidDAO, null);

        assertThrows(ValidationException.class, () -> service.updateAutobid(5, 1, 150.0, 180.0));
        assertEquals(200.0, autoBidDAO.findAutoBidById(1).getMaxPrice());
    }

    private static final class InMemoryAutoBidDAO implements AutoBidDAO {
        private final List<AutoBid> autoBids = new ArrayList<>();
        private int nextId = 1;

        @Override
        public void saveAutoBid(AutoBid item) {
            if (item.getId() <= 0) {
                item.setId(nextId++);
            }
            autoBids.add(item);
        }

        @Override
        public void updateAutoBid(AutoBid item) {
        }

        @Override
        public void deleteAutoBid(int id) {
            autoBids.removeIf(autoBid -> autoBid.getId() == id);
        }

        @Override
        public AutoBid findAutoBidById(int id) {
            return autoBids.stream()
                    .filter(autoBid -> autoBid.getId() == id)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public AutoBid findAutoBidByAuctionBidder(int auctionId, int bidderId) {
            return autoBids.stream()
                    .filter(autoBid -> autoBid.getAuctionId() == auctionId && autoBid.getBidderId() == bidderId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<AutoBid> findAutoBidsByAuction(int auctionId) {
            return autoBids.stream()
                    .filter(autoBid -> autoBid.getAuctionId() == auctionId)
                    .toList();
        }

        @Override
        public List<AutoBid> findAllUserAutoBid(int bidderId) {
            return autoBids.stream()
                    .filter(autoBid -> autoBid.getBidderId() == bidderId)
                    .toList();
        }
    }
}
