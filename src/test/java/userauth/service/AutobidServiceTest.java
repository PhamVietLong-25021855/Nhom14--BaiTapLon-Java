package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutobidServiceTest {

    @Test
    void sellerCannotCreateAutobidForOwnAuction() {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        auctionDAO.putAuction(new AuctionItem(
                1,
                "Painting",
                "Modern art",
                100_000,
                120_000,
                System.currentTimeMillis() - 10_000,
                System.currentTimeMillis() + 120_000,
                "Art",
                null,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                5,
                -1,
                AuctionStatus.RUNNING
        ));
        AutobidService service = new AutobidService(new InMemoryAutoBidDAO(), auctionDAO);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.createAutobid(5, 1, 150_000, 10_000)
        );

        assertEquals("Sellers cannot create auto-bids for their own auctions.", ex.getMessage());
    }

    @Test
    void createAutobidRequiresMaxPriceAboveCurrentBid() {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        auctionDAO.putAuction(new AuctionItem(
                1,
                "Phone",
                "Flagship phone",
                100_000,
                140_000,
                System.currentTimeMillis() - 10_000,
                System.currentTimeMillis() + 120_000,
                "Electronics",
                null,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                9,
                3,
                AuctionStatus.RUNNING
        ));
        AutobidService service = new AutobidService(new InMemoryAutoBidDAO(), auctionDAO);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.createAutobid(8, 1, 140_000, 5_000)
        );

        assertEquals("Max price must be higher than the current highest bid.", ex.getMessage());
    }

    private static final class InMemoryAuctionDAO implements AuctionDAO {
        private final Map<Integer, AuctionItem> auctions = new HashMap<>();

        void putAuction(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        @Override
        public void saveAuction(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        @Override
        public void updateAuction(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        @Override
        public void deleteAuction(int id) {
            auctions.remove(id);
        }

        @Override
        public AuctionItem findAuctionById(int id) {
            return auctions.get(id);
        }

        @Override
        public List<AuctionItem> findAllAuctions() {
            return List.copyOf(auctions.values());
        }

        @Override
        public List<AuctionItem> findAuctionsBySeller(int sellerId) {
            return auctions.values().stream().filter(item -> item.getSellerId() == sellerId).toList();
        }

        @Override
        public void saveBid(BidTransaction bid) {
        }

        @Override
        public void saveBidAndUpdateAuction(BidTransaction bid, AuctionItem item) {
        }

        @Override
        public boolean markAuctionFinished(int auctionId, long endTime, long updatedAt) {
            return false;
        }

        @Override
        public List<BidTransaction> findAllBids() {
            return List.of();
        }

        @Override
        public List<BidTransaction> findBidsByAuction(int auctionId) {
            return List.of();
        }
    }

    private static final class InMemoryAutoBidDAO implements AutoBidDAO {
        @Override
        public void saveAutoBid(AutoBid item) {
        }

        @Override
        public void updateAutoBid(AutoBid item) {
        }

        @Override
        public void deleteAutoBid(int id) {
        }

        @Override
        public AutoBid findAutoBidById(int id) {
            return null;
        }

        @Override
        public AutoBid findAutoBidByAuctionBidder(int auction_id, int bidder_id) {
            return null;
        }

        @Override
        public List<AutoBid> findAllUserAutoBid(int bidderId) {
            return List.of();
        }

        @Override
        public List<AutoBid> findAutoBidsByAuction(int auctionId) {
            return List.of();
        }
    }
}
