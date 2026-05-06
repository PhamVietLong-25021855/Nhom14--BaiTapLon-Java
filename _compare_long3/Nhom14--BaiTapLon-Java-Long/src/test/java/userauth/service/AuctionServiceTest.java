package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionServiceTest {

    @Test
    void placeBidExtendsAuctionWhenBidArrivesInsideSnipingWindow()
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Phone",
                "Flagship",
                100.0,
                100.0,
                now - 60_000,
                now + 5_000,
                "Electronics",
                null,
                null,
                now - 60_000,
                now - 60_000,
                8,
                -1,
                AuctionStatus.RUNNING
        );

        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO(auction);
        InMemoryAutoBidDAO autoBidDAO = new InMemoryAutoBidDAO();
        AuctionService service = new AuctionService(auctionDAO, autoBidDAO);

        service.placeBid(1, 77, 120.0);

        AuctionItem updatedAuction = auctionDAO.findAuctionById(1);
        assertEquals(77, updatedAuction.getWinnerId());
        assertEquals(120.0, updatedAuction.getCurrentHighestBid());
        assertTrue(updatedAuction.getEndTime() >= now + 30_000);
        assertEquals(1, auctionDAO.findBidsByAuction(1).size());
    }

    @Test
    void placeBidPrefersEarlierAutoBidRegistrationWhenMaxPricesTie()
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Laptop",
                "Gaming",
                100.0,
                100.0,
                now - 60_000,
                now + 120_000,
                "Electronics",
                null,
                null,
                now - 60_000,
                now - 60_000,
                9,
                -1,
                AuctionStatus.RUNNING
        );

        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO(auction);
        InMemoryAutoBidDAO autoBidDAO = new InMemoryAutoBidDAO();
        autoBidDAO.autoBids.add(new AutoBid(1, 1, 10, 200.0, 10.0, 1_000L, 1_000L));
        autoBidDAO.autoBids.add(new AutoBid(2, 1, 11, 200.0, 10.0, 2_000L, 2_000L));

        AuctionService service = new AuctionService(auctionDAO, autoBidDAO);

        service.placeBid(1, 20, 110.0);

        AuctionItem updatedAuction = auctionDAO.findAuctionById(1);
        List<BidTransaction> bids = auctionDAO.findBidsByAuction(1);
        BidTransaction finalBid = bids.stream()
                .max(Comparator.comparingLong(BidTransaction::getTimestamp))
                .orElseThrow();

        assertEquals(10, updatedAuction.getWinnerId());
        assertEquals(200.0, updatedAuction.getCurrentHighestBid());
        assertEquals(10, finalBid.getBidderId());
        assertEquals(200.0, finalBid.getAmount());
    }

    @Test
    void placeBidRejectsBidThatDoesNotBeatCurrentPrice() {
        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Watch",
                "Limited",
                100.0,
                150.0,
                now - 60_000,
                now + 60_000,
                "Luxury",
                null,
                null,
                now - 60_000,
                now - 60_000,
                5,
                99,
                AuctionStatus.RUNNING
        );

        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO(auction);
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO());

        assertThrows(InvalidBidException.class, () -> service.placeBid(1, 88, 150.0));
    }

    @Test
    void placeBidSupportsMultipleAutoBidRulesAndResolvesConflictDeterministically()
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Camera",
                "Mirrorless",
                100.0,
                100.0,
                now - 60_000,
                now + 120_000,
                "Electronics",
                null,
                null,
                now - 60_000,
                now - 60_000,
                15,
                -1,
                AuctionStatus.RUNNING
        );

        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO(auction);
        InMemoryAutoBidDAO autoBidDAO = new InMemoryAutoBidDAO();
        autoBidDAO.autoBids.add(new AutoBid(1, 1, 10, 150.0, 10.0, 1_000L, 1_000L));
        autoBidDAO.autoBids.add(new AutoBid(2, 1, 11, 180.0, 20.0, 2_000L, 2_000L));
        autoBidDAO.autoBids.add(new AutoBid(3, 1, 12, 220.0, 15.0, 3_000L, 3_000L));

        AuctionService service = new AuctionService(auctionDAO, autoBidDAO);

        service.placeBid(1, 21, 110.0);

        AuctionItem updatedAuction = auctionDAO.findAuctionById(1);
        List<BidTransaction> bids = auctionDAO.findBidsByAuction(1);

        assertEquals(12, updatedAuction.getWinnerId());
        assertEquals(195.0, updatedAuction.getCurrentHighestBid());
        assertEquals(6, bids.size());
        assertEquals(List.of(21, 12, 11, 12, 11, 12),
                bids.stream().map(BidTransaction::getBidderId).toList());
    }

    @Test
    void placeBidStopsExtendingAfterAntiSnipingLimit()
            throws Exception {
        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Console",
                "Collector",
                100.0,
                100.0,
                now - 60_000,
                now + 5_000,
                "Games",
                null,
                null,
                now - 60_000,
                now - 60_000,
                30,
                -1,
                AuctionStatus.RUNNING
        );

        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO(auction);
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO());

        service.placeBid(1, 101, 110.0);
        long firstExtendedEnd = auctionDAO.findAuctionById(1).getEndTime();
        Thread.sleep(5L);
        service.placeBid(1, 102, 120.0);
        Thread.sleep(5L);
        service.placeBid(1, 103, 130.0);
        long thirdExtendedEnd = auctionDAO.findAuctionById(1).getEndTime();
        Thread.sleep(5L);
        service.placeBid(1, 104, 140.0);

        AuctionItem updatedAuction = auctionDAO.findAuctionById(1);

        assertTrue(updatedAuction.getAntiSnipingExtensionCount() <= AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS);
        assertEquals(AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS, updatedAuction.getAntiSnipingExtensionCount());
        assertTrue(thirdExtendedEnd >= firstExtendedEnd);
        assertEquals(thirdExtendedEnd, updatedAuction.getEndTime());
    }

    @Test
    void markAuctionAsPaidTransitionsFinishedAuctionToPaid()
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Artwork",
                "Oil on canvas",
                100.0,
                180.0,
                now - 120_000,
                now - 60_000,
                "Art",
                null,
                null,
                now - 120_000,
                now - 60_000,
                50,
                88,
                AuctionStatus.FINISHED
        );

        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO(auction);
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO());

        service.markAuctionAsPaid(1, 50);

        assertEquals(AuctionStatus.PAID, auctionDAO.findAuctionById(1).getStatus());
    }

    @Test
    void cancelFinishedAuctionClearsWinnerAndMovesToCanceled()
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Artwork",
                "Oil on canvas",
                100.0,
                180.0,
                now - 120_000,
                now - 60_000,
                "Art",
                null,
                null,
                now - 120_000,
                now - 60_000,
                50,
                88,
                AuctionStatus.FINISHED
        );

        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO(auction);
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO());

        service.cancelFinishedAuction(1, 50);

        AuctionItem updatedAuction = auctionDAO.findAuctionById(1);
        assertEquals(AuctionStatus.CANCELED, updatedAuction.getStatus());
        assertEquals(-1, updatedAuction.getWinnerId());
    }

    private static final class InMemoryAuctionDAO implements AuctionDAO {
        private AuctionItem auction;
        private final List<BidTransaction> bids = new ArrayList<>();
        private int nextBidId = 1;

        private InMemoryAuctionDAO(AuctionItem auction) {
            this.auction = auction;
        }

        @Override
        public void saveAuction(AuctionItem item) {
            this.auction = item;
        }

        @Override
        public void updateAuction(AuctionItem item) {
            this.auction = item;
        }

        @Override
        public void deleteAuction(int id) {
            if (auction != null && auction.getId() == id) {
                auction = null;
            }
        }

        @Override
        public AuctionItem findAuctionById(int id) {
            return auction != null && auction.getId() == id ? auction : null;
        }

        @Override
        public List<AuctionItem> findAllAuctions() {
            return auction == null ? List.of() : List.of(auction);
        }

        @Override
        public void saveBid(BidTransaction bid) {
            bid.setId(nextBidId++);
            bids.add(bid);
        }

        @Override
        public List<BidTransaction> findAllBids() {
            return new ArrayList<>(bids);
        }

        @Override
        public List<BidTransaction> findBidsByAuction(int auctionId) {
            return bids.stream()
                    .filter(bid -> bid.getAuctionId() == auctionId)
                    .toList();
        }
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
