package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.dao.WalletDAO;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidRequest;
import userauth.model.BidResult;
import userauth.model.BidTransaction;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;
import userauth.realtime.AuctionEventType;
import userauth.realtime.AuctionEventPublisher;
import userauth.realtime.AuctionUpdateEvent;
import userauth.realtime.NoOpAuctionEventPublisher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {

    @Test
    void createAuctionStoresSellerAntiSnipingConfiguration() throws ValidationException {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        WalletService walletService = new WalletService(new InMemoryWalletDAO());
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO(), walletService);

        long now = System.currentTimeMillis();
        long startTime = now + 60_000;
        long endTime = startTime + 1_800_000;

        service.createAuction(
                "Camera",
                "Mirrorless camera",
                2_000_000,
                startTime,
                endTime,
                "Electronics",
                null,
                77,
                true,
                45,
                90,
                4
        );

        AuctionItem created = auctionDAO.findAllAuctions().getFirst();
        assertTrue(created.isAntiSnipingEnabled());
        assertEquals(45, created.getExtensionThresholdSeconds());
        assertEquals(90, created.getExtensionDurationSeconds());
        assertEquals(4, created.getMaxExtensionCount());
        assertEquals(endTime, created.getOriginalEndTime());
    }

    @Test
    void updateAuctionStoresSellerAntiSnipingConfiguration()
            throws ValidationException, ItemNotFoundException, UnauthorizedException {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        WalletService walletService = new WalletService(new InMemoryWalletDAO());
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO(), walletService);

        long now = System.currentTimeMillis();
        AuctionItem auction = scheduledAuction(1, 80, now + 120_000, now + 3_600_000, 500_000);
        auctionDAO.saveAuction(auction);

        service.updateAuction(
                auction.getId(),
                80,
                "Updated Camera",
                "Updated description",
                600_000,
                now + 180_000,
                now + 4_200_000,
                "Electronics",
                null,
                false,
                20,
                40,
                2
        );

        AuctionItem updated = auctionDAO.findAuctionById(auction.getId());
        assertFalse(updated.isAntiSnipingEnabled());
        assertEquals(20, updated.getExtensionThresholdSeconds());
        assertEquals(40, updated.getExtensionDurationSeconds());
        assertEquals(2, updated.getMaxExtensionCount());
        assertEquals(updated.getEndTime(), updated.getOriginalEndTime());
        assertEquals(0, updated.getExtensionCount());
    }

    @Test
    void sellerCanManuallyExtendAuctionTime()
            throws ValidationException, ItemNotFoundException, UnauthorizedException {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        WalletService walletService = new WalletService(new InMemoryWalletDAO());
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO(), walletService);

        long now = System.currentTimeMillis();
        long originalEndTime = now + 120_000;
        AuctionItem auction = runningAuction(1, 88, now - 60_000, originalEndTime, 500_000);
        auctionDAO.saveAuction(auction);

        service.extendAuctionTime(auction.getId(), 88, 15);

        AuctionItem updated = auctionDAO.findAuctionById(auction.getId());
        assertEquals(originalEndTime + 15 * 60_000L, updated.getEndTime());
        assertEquals(originalEndTime, updated.getOriginalEndTime());
        assertEquals(0, updated.getExtensionCount());
        assertEquals(AuctionStatus.RUNNING, updated.getStatus());
    }

    @Test
    void sellerCannotManuallyExtendFinishedAuction() {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        WalletService walletService = new WalletService(new InMemoryWalletDAO());
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO(), walletService);

        long now = System.currentTimeMillis();
        AuctionItem auction = new AuctionItem(
                1,
                "Old Item",
                "Already finished",
                400_000,
                400_000,
                now - 3_600_000,
                now - 1_000,
                "Electronics",
                null,
                now - 3_600_000,
                now - 1_000,
                55,
                -1,
                AuctionStatus.FINISHED
        );
        auctionDAO.saveAuction(auction);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.extendAuctionTime(auction.getId(), 55, 10)
        );

        assertEquals("This auction can only be extended while it is OPEN or RUNNING.", ex.getMessage());
    }

    @Test
    void sellerCannotBidOnOwnAuction() {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        WalletService walletService = new WalletService(walletDAO);
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO(), walletService);

        long now = System.currentTimeMillis();
        AuctionItem auction = runningAuction(1, 10, now - 5_000, now + 120_000, 100_000);
        auctionDAO.saveAuction(auction);
        walletDAO.putWallet(10, 500_000);

        InvalidBidException ex = assertThrows(
                InvalidBidException.class,
                () -> service.placeBid(auction.getId(), 10, 120_000)
        );

        assertEquals("Sellers cannot place bids on their own auctions.", ex.getMessage());
        assertTrue(auctionDAO.findBidsByAuction(auction.getId()).isEmpty());
    }

    @Test
    void bidNearAuctionEndExtendsEndTimeAndUpdatesWinner()
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        WalletService walletService = new WalletService(walletDAO);
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO(), walletService);

        long now = System.currentTimeMillis();
        long originalEndTime = now + 10_000;
        AuctionItem auction = runningAuction(1, 99, now - 60_000, originalEndTime, 100_000);
        auctionDAO.saveAuction(auction);
        walletDAO.putWallet(20, 500_000);

        service.placeBid(auction.getId(), 20, 120_000);

        AuctionItem updated = auctionDAO.findAuctionById(auction.getId());
        assertEquals(120_000, updated.getCurrentHighestBid());
        assertEquals(20, updated.getWinnerId());
        assertTrue(updated.getEndTime() >= originalEndTime + 60_000);
        assertEquals(1, auctionDAO.findBidsByAuction(auction.getId()).size());
    }

    @Test
    void bidOutsideThresholdDoesNotExtendAuctionAndPublishesBidAcceptedOnly()
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        RecordingAuctionEventPublisher eventPublisher = new RecordingAuctionEventPublisher();
        WalletService walletService = new WalletService(walletDAO);
        AuctionService service = new AuctionService(
                auctionDAO,
                new InMemoryAutoBidDAO(),
                walletService,
                new AuctionLockManager(),
                new AntiSnipingService(),
                eventPublisher
        );

        long now = System.currentTimeMillis();
        long originalEndTime = now + 120_000;
        AuctionItem auction = runningAuction(1, 61, now - 30_000, originalEndTime, 100_000);
        configureAntiSniping(auction, true, 30, 60, 5);
        auctionDAO.saveAuction(auction);
        walletDAO.putWallet(31, 500_000);

        BidResult result = service.placeBid(new BidRequest(auction.getId(), 31, 120_000));
        AuctionItem updated = auctionDAO.findAuctionById(auction.getId());

        assertFalse(result.isAuctionExtended());
        assertEquals(originalEndTime, updated.getEndTime());
        assertEquals(1, eventPublisher.events.size());
        assertEquals(AuctionEventType.BID_ACCEPTED, eventPublisher.events.getFirst().getEventType());
    }

    @Test
    void invalidBidDoesNotExtendAuctionAndDoesNotPublishEvent() {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        RecordingAuctionEventPublisher eventPublisher = new RecordingAuctionEventPublisher();
        WalletService walletService = new WalletService(walletDAO);
        AuctionService service = new AuctionService(
                auctionDAO,
                new InMemoryAutoBidDAO(),
                walletService,
                new AuctionLockManager(),
                new AntiSnipingService(),
                eventPublisher
        );

        long now = System.currentTimeMillis();
        long originalEndTime = now + 10_000;
        AuctionItem auction = runningAuction(1, 62, now - 30_000, originalEndTime, 100_000);
        configureAntiSniping(auction, true, 30, 60, 5);
        auctionDAO.saveAuction(auction);
        walletDAO.putWallet(32, 500_000);

        InvalidBidException ex = assertThrows(
                InvalidBidException.class,
                () -> service.placeBid(new BidRequest(auction.getId(), 32, 100_000))
        );

        AuctionItem updated = auctionDAO.findAuctionById(auction.getId());
        assertEquals("The amount must be higher than the starting price (100000.0).", ex.getMessage());
        assertEquals(originalEndTime, updated.getEndTime());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void maxExtensionCountPreventsFurtherExtension()
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        RecordingAuctionEventPublisher eventPublisher = new RecordingAuctionEventPublisher();
        WalletService walletService = new WalletService(walletDAO);
        AuctionService service = new AuctionService(
                auctionDAO,
                new InMemoryAutoBidDAO(),
                walletService,
                new AuctionLockManager(),
                new AntiSnipingService(),
                eventPublisher
        );

        long now = System.currentTimeMillis();
        long originalEndTime = now + 10_000;
        AuctionItem auction = runningAuction(1, 63, now - 30_000, originalEndTime, 100_000);
        configureAntiSniping(auction, true, 30, 60, 1);
        auction.setExtensionCount(1);
        auctionDAO.saveAuction(auction);
        walletDAO.putWallet(33, 500_000);

        BidResult result = service.placeBid(new BidRequest(auction.getId(), 33, 120_000));
        AuctionItem updated = auctionDAO.findAuctionById(auction.getId());

        assertFalse(result.isAuctionExtended());
        assertEquals(originalEndTime, updated.getEndTime());
        assertEquals(1, updated.getExtensionCount());
        assertEquals(1, eventPublisher.events.size());
        assertEquals(AuctionEventType.BID_ACCEPTED, eventPublisher.events.getFirst().getEventType());
    }

    @Test
    void manualCloseMarksAuctionPaidAndDeductsWinnerWallet()
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException, UnauthorizedException {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        WalletService walletService = new WalletService(walletDAO);
        AuctionService service = new AuctionService(auctionDAO, new InMemoryAutoBidDAO(), walletService);

        long now = System.currentTimeMillis();
        AuctionItem auction = runningAuction(1, 7, now - 30_000, now + 120_000, 100_000);
        auctionDAO.saveAuction(auction);
        walletDAO.putWallet(21, 500_000);

        service.placeBid(auction.getId(), 21, 150_000);
        service.closeAuctionManually(auction.getId(), 7);

        AuctionItem closed = auctionDAO.findAuctionById(auction.getId());
        Wallet winnerWallet = walletDAO.findWalletByUserId(21);
        assertEquals(AuctionStatus.PAID, closed.getStatus());
        assertEquals(350_000, winnerWallet.getBalance());
    }

    @Test
    void concurrentSameAmountBidsOnlyAcceptOneWinner() throws Exception {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        WalletService walletService = new WalletService(walletDAO);
        AuctionService service = new AuctionService(
                auctionDAO,
                new InMemoryAutoBidDAO(),
                walletService,
                new AuctionLockManager()
        );

        long now = System.currentTimeMillis();
        AuctionItem auction = runningAuction(1, 50, now - 5_000, now + 120_000, 100_000);
        auctionDAO.saveAuction(auction);

        int threadCount = 20;
        double sameBidAmount = 110_000;
        for (int i = 0; i < threadCount; i++) {
            walletDAO.putWallet(100 + i, 500_000);
        }

        AtomicInteger acceptedCount = new AtomicInteger();
        List<String> rejectedMessages = new CopyOnWriteArrayList<>();
        runConcurrentBidScenario(threadCount, bidderIndex -> {
            int bidderId = 100 + bidderIndex;
            try {
                BidResult result = service.placeBid(new BidRequest(auction.getId(), bidderId, sameBidAmount));
                if (result.isAccepted()) {
                    acceptedCount.incrementAndGet();
                }
            } catch (InvalidBidException ex) {
                rejectedMessages.add(ex.getMessage());
            }
        });

        AuctionItem updated = auctionDAO.findAuctionById(auction.getId());
        List<BidTransaction> history = auctionDAO.findBidsByAuction(auction.getId());

        assertEquals(1, acceptedCount.get());
        assertEquals(threadCount - 1, rejectedMessages.size());
        assertEquals(sameBidAmount, updated.getCurrentHighestBid());
        assertEquals(1, history.size());
        assertEquals(updated.getWinnerId(), history.getFirst().getBidderId());
    }

    @Test
    void concurrentIncreasingBidsPreserveHighestBidAndHistory() throws Exception {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        WalletService walletService = new WalletService(walletDAO);
        AuctionService service = new AuctionService(
                auctionDAO,
                new InMemoryAutoBidDAO(),
                walletService,
                new AuctionLockManager()
        );

        long now = System.currentTimeMillis();
        AuctionItem auction = runningAuction(1, 51, now - 5_000, now + 120_000, 100_000);
        auctionDAO.saveAuction(auction);

        int threadCount = 25;
        double baseBidAmount = 110_000;
        double bidStep = 5_000;
        AtomicInteger acceptedCount = new AtomicInteger();
        List<Throwable> unexpectedFailures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            walletDAO.putWallet(200 + i, 1_000_000);
        }

        runConcurrentBidScenario(threadCount, bidderIndex -> {
            int bidderId = 200 + bidderIndex;
            double amount = baseBidAmount + (bidderIndex * bidStep);
            try {
                BidResult result = service.placeBid(new BidRequest(auction.getId(), bidderId, amount));
                if (result.isAccepted()) {
                    acceptedCount.incrementAndGet();
                }
            } catch (InvalidBidException ignored) {
                // Lower bids can legitimately lose the race against a higher accepted bid.
            } catch (ItemNotFoundException | AuctionClosedException ex) {
                unexpectedFailures.add(ex);
            }
        });

        AuctionItem updated = auctionDAO.findAuctionById(auction.getId());
        List<BidTransaction> history = auctionDAO.findBidsByAuction(auction.getId());
        double expectedHighest = baseBidAmount + ((threadCount - 1) * bidStep);
        int expectedWinnerId = 200 + (threadCount - 1);

        assertTrue(unexpectedFailures.isEmpty());
        assertEquals(expectedHighest, updated.getCurrentHighestBid());
        assertEquals(expectedWinnerId, updated.getWinnerId());
        assertEquals(acceptedCount.get(), history.size());
        assertEquals(
                expectedHighest,
                history.stream().mapToDouble(BidTransaction::getAmount).max().orElseThrow()
        );
        assertEquals(
                history.size(),
                history.stream().map(BidTransaction::getId).distinct().count()
        );
    }

    private static void runConcurrentBidScenario(int threadCount, ConcurrentBidAction action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Throwable> taskFailures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                int bidderIndex = i;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        assertTrue(start.await(5, TimeUnit.SECONDS));
                        action.run(bidderIndex);
                    } catch (Throwable ex) {
                        taskFailures.add(ex);
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
            assertTrue(taskFailures.isEmpty(), "Concurrent task failures: " + taskFailures);
        } finally {
            executor.shutdownNow();
        }
    }

    private static AuctionItem runningAuction(int id, int sellerId, long startTime, long endTime, double startPrice) {
        return new AuctionItem(
                id,
                "Laptop",
                "Gaming laptop",
                startPrice,
                startPrice,
                startTime,
                endTime,
                "Electronics",
                null,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                sellerId,
                -1,
                AuctionStatus.RUNNING
        );
    }

    private static AuctionItem scheduledAuction(int id, int sellerId, long startTime, long endTime, double startPrice) {
        return new AuctionItem(
                id,
                "Draft Item",
                "Scheduled draft",
                startPrice,
                startPrice,
                startTime,
                endTime,
                "Electronics",
                null,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                sellerId,
                -1,
                AuctionStatus.OPEN
        );
    }

    private static void configureAntiSniping(AuctionItem auction, boolean enabled, int thresholdSeconds,
                                             int durationSeconds, int maxExtensionCount) {
        auction.setAntiSnipingEnabled(enabled);
        auction.setOriginalEndTime(auction.getEndTime());
        auction.setExtensionThresholdSeconds(thresholdSeconds);
        auction.setExtensionDurationSeconds(durationSeconds);
        auction.setMaxExtensionCount(maxExtensionCount);
        auction.setExtensionCount(0);
    }

    private static final class InMemoryAuctionDAO implements AuctionDAO {
        private final Map<Integer, AuctionItem> auctions = new ConcurrentHashMap<>();
        private final List<BidTransaction> bids = Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger nextAuctionId = new AtomicInteger(1);
        private final AtomicInteger nextBidId = new AtomicInteger(1);

        @Override
        public void saveAuction(AuctionItem item) {
            if (item.getId() <= 0) {
                item.setId(nextAuctionId.getAndIncrement());
            }
            auctions.put(item.getId(), copyAuction(item));
        }

        @Override
        public void updateAuction(AuctionItem item) {
            auctions.put(item.getId(), copyAuction(item));
        }

        @Override
        public void deleteAuction(int id) {
            auctions.remove(id);
            bids.removeIf(bid -> bid.getAuctionId() == id);
        }

        @Override
        public AuctionItem findAuctionById(int id) {
            AuctionItem item = auctions.get(id);
            return item == null ? null : copyAuction(item);
        }

        @Override
        public List<AuctionItem> findAllAuctions() {
            return auctions.values().stream()
                    .map(InMemoryAuctionDAO::copyAuction)
                    .sorted(Comparator.comparingInt(AuctionItem::getId))
                    .toList();
        }

        @Override
        public List<AuctionItem> findAuctionsBySeller(int sellerId) {
            return auctions.values().stream()
                    .filter(item -> item.getSellerId() == sellerId)
                    .map(InMemoryAuctionDAO::copyAuction)
                    .sorted(Comparator.comparingInt(AuctionItem::getId))
                    .toList();
        }

        @Override
        public void saveBid(BidTransaction bid) {
            if (bid.getId() <= 0) {
                bid.setId(nextBidId.getAndIncrement());
            }
            bids.add(copyBid(bid));
        }

        @Override
        public void saveBidAndUpdateAuction(BidTransaction bid, AuctionItem item) {
            saveBid(bid);
            updateAuction(item);
        }

        @Override
        public boolean markAuctionFinished(int auctionId, long endTime, long updatedAt) {
            AuctionItem item = auctions.get(auctionId);
            if (item == null) {
                return false;
            }
            if (item.getStatus() != AuctionStatus.OPEN && item.getStatus() != AuctionStatus.RUNNING) {
                return false;
            }
            item.setStatus(AuctionStatus.FINISHED);
            item.setEndTime(endTime);
            item.setUpdatedAt(updatedAt);
            auctions.put(auctionId, copyAuction(item));
            return true;
        }

        @Override
        public List<BidTransaction> findAllBids() {
            return bids.stream().map(InMemoryAuctionDAO::copyBid).toList();
        }

        @Override
        public List<BidTransaction> findBidsByAuction(int auctionId) {
            return bids.stream()
                    .filter(bid -> bid.getAuctionId() == auctionId)
                    .map(InMemoryAuctionDAO::copyBid)
                    .toList();
        }

        private static AuctionItem copyAuction(AuctionItem item) {
            AuctionItem copy = new AuctionItem(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getStartPrice(),
                    item.getCurrentHighestBid(),
                    item.getStartTime(),
                    item.getEndTime(),
                    item.getOriginalEndTime(),
                    item.getExtensionCount(),
                    item.getMaxExtensionCount(),
                    item.isAntiSnipingEnabled(),
                    item.getExtensionThresholdSeconds(),
                    item.getExtensionDurationSeconds(),
                    item.getCategory(),
                    item.getImageSource(),
                    item.getCreatedAt(),
                    item.getUpdatedAt(),
                    item.getSellerId(),
                    item.getWinnerId(),
                    item.getStatus()
            );
            return copy;
        }

        private static BidTransaction copyBid(BidTransaction bid) {
            return new BidTransaction(
                    bid.getId(),
                    bid.getAuctionId(),
                    bid.getBidderId(),
                    bid.getAmount(),
                    bid.getTimestamp(),
                    bid.getStatus()
            );
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

    private static final class InMemoryWalletDAO implements WalletDAO {
        private final Map<Integer, Wallet> wallets = new ConcurrentHashMap<>();
        private final AtomicInteger nextWalletId = new AtomicInteger(1);

        void putWallet(int userId, double balance) {
            Wallet wallet = new Wallet(
                    nextWalletId.getAndIncrement(),
                    userId,
                    balance,
                    System.currentTimeMillis(),
                    System.currentTimeMillis()
            );
            wallets.put(userId, copyWallet(wallet));
        }

        @Override
        public int saveWallet(Wallet wallet) {
            if (wallet.getId() <= 0) {
                wallet.setId(nextWalletId.getAndIncrement());
            }
            wallets.put(wallet.getUserID(), copyWallet(wallet));
            return wallet.getId();
        }

        @Override
        public void updateWallet(Wallet wallet) {
            wallets.put(wallet.getUserID(), copyWallet(wallet));
        }

        @Override
        public Wallet findWalletByUserId(int userId) {
            Wallet wallet = wallets.get(userId);
            return wallet == null ? null : copyWallet(wallet);
        }

        @Override
        public void deleteWallet(int walledId) {
        }

        @Override
        public int saveTopUpTransaction(TopUpTransaction topUpTransaction) {
            return 0;
        }

        @Override
        public void updateTopUpTransaction(TopUpTransaction transaction) {
        }

        @Override
        public TopUpTransaction findTopUpTransactionById(int transactionId) {
            return null;
        }

        @Override
        public List<TopUpTransaction> findTopUpTransactionsByUserId(int userId) {
            return List.of();
        }

        @Override
        public List<TopUpTransaction> findAllPendingTransactions() {
            return List.of();
        }

        @Override
        public void deleteTopUpTransaction(int transactionID) {
        }

        private static Wallet copyWallet(Wallet wallet) {
            Wallet copy = new Wallet(
                    wallet.getId(),
                    wallet.getUserID(),
                    wallet.getBalance(),
                    wallet.getCreatedAt(),
                    wallet.getUpdatedAt()
            );
            copy.setId(wallet.getId());
            return copy;
        }
    }

    private static final class RecordingAuctionEventPublisher implements AuctionEventPublisher {
        private final List<AuctionUpdateEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void publish(AuctionUpdateEvent event) {
            events.add(event);
        }
    }

    @FunctionalInterface
    private interface ConcurrentBidAction {
        void run(int bidderIndex) throws Exception;
    }
}
