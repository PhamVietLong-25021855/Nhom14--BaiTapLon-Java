package userauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.exception.InvalidBidException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {
    private InMemoryAuctionDao auctionDao;
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionDao = new InMemoryAuctionDao();
        auctionService = new AuctionService(auctionDao, new EmptyAutoBidDao());
    }

    @Test
    void placeBidRejectsAmountLowerThanCurrentPrice() {
        AuctionItem item = runningAuction(1, 100.0, 150.0);
        auctionDao.saveAuction(item);

        assertThrows(InvalidBidException.class, () -> auctionService.placeBid(1, 20, 150.0));
    }

    @Test
    void concurrentBidsKeepOnlyHighestWinnerAndNoLostUpdate() throws Exception {
        AuctionItem item = runningAuction(1, 100.0, 100.0);
        auctionDao.saveAuction(item);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Void>> tasks = new ArrayList<>();
        double[] amounts = {120, 130, 140, 150, 160, 170};
        for (int i = 0; i < amounts.length; i++) {
            int bidderId = i + 10;
            double amount = amounts[i];
            tasks.add(() -> {
                start.await();
                try {
                    auctionService.placeBid(1, bidderId, amount);
                } catch (InvalidBidException ignored) {
                    // A lower bid can become invalid if a higher bid wins the lock first.
                }
                return null;
            });
        }

        List<Future<Void>> futures = tasks.stream().map(pool::submit).toList();
        start.countDown();
        for (Future<Void> future : futures) {
            future.get(3, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        AuctionItem updated = auctionDao.findAuctionById(1);
        assertEquals(170.0, updated.getCurrentHighestBid());
        assertEquals(15, updated.getWinnerId());
        assertTrue(auctionDao.findBidsByAuction(1).size() >= 1);
    }

    private AuctionItem runningAuction(int id, double startPrice, double currentHighestBid) {
        long now = System.currentTimeMillis();
        return new AuctionItem(
                id,
                "Laptop",
                "Test auction",
                startPrice,
                currentHighestBid,
                now - 1_000,
                now + 60_000,
                "Electronics",
                now,
                now,
                1,
                -1,
                AuctionStatus.RUNNING
        );
    }

    private static final class InMemoryAuctionDao implements AuctionDAO {
        private final AtomicInteger auctionIds = new AtomicInteger(1);
        private final AtomicInteger bidIds = new AtomicInteger(1);
        private final Map<Integer, AuctionItem> auctions = new ConcurrentHashMap<>();
        private final List<BidTransaction> bids = new CopyOnWriteArrayList<>();

        @Override
        public void saveAuction(AuctionItem item) {
            if (item.getId() <= 0) {
                item.setId(auctionIds.getAndIncrement());
            }
            auctions.put(item.getId(), item);
        }

        @Override
        public void updateAuction(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        @Override
        public void deleteAuction(int id) {
            auctions.remove(id);
            bids.removeIf(bid -> bid.getAuctionId() == id);
        }

        @Override
        public AuctionItem findAuctionById(int id) {
            return auctions.get(id);
        }

        @Override
        public List<AuctionItem> findAllAuctions() {
            return new ArrayList<>(auctions.values());
        }

        @Override
        public void saveBid(BidTransaction bid) {
            bid.setId(bidIds.getAndIncrement());
            bids.add(bid);
        }

        @Override
        public List<BidTransaction> findAllBids() {
            return new ArrayList<>(bids);
        }

        @Override
        public List<BidTransaction> findBidsByAuction(int auctionId) {
            return bids.stream().filter(bid -> bid.getAuctionId() == auctionId).toList();
        }
    }

    private static final class EmptyAutoBidDao implements AutoBidDAO {
        public void saveAutoBid(AutoBid item) {}
        public void updateAutoBid(AutoBid item) {}
        public void deleteAutoBid(int id) {}
        public AutoBid findAutoBidById(int id) { return null; }
        public AutoBid findAutoBidByAuctionBidder(int auctionId, int bidderId) { return null; }
        public List<AutoBid> findAutoBidsByAuction(int auctionId) { return List.of(); }
        public List<AutoBid> findAllUserAutoBid(int bidderId) { return List.of(); }
    }
}
