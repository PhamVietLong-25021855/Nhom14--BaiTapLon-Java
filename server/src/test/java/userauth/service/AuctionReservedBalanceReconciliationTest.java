package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.dao.WalletDAO;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;
import userauth.model.WalletTransaction;
import userauth.model.WalletTransactionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionReservedBalanceReconciliationTest {
    @Test
    void canceledAuctionIsExcludedFromReservedBalance() {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        ServiceTestSupport.EmptyNotificationDAO notificationDAO = new ServiceTestSupport.EmptyNotificationDAO();

        AuctionService auctionService = new AuctionService(auctionDAO, new EmptyAutoBidDAO(), new WalletService(walletDAO), new NotificationService(notificationDAO));

        int bidderId = 7;
        walletDAO.putWallet(new Wallet(1, bidderId, 1_000_000L, 250_000L, 1L, 1L));
        auctionDAO.putAuction(auction(
                11,
                5,
                -1,
                250_000.0,
                AuctionStatus.CANCELED
        ));
        auctionDAO.putBid(new BidTransaction(1, 11, bidderId, 250_000.0, 10L));

        auctionService.reconcileReservedBalances();

        Wallet wallet = walletDAO.findWalletByUserId(bidderId);
        assertEquals(0L, wallet.getReservedBalance());
        assertEquals(1_000_000L, wallet.getAvailableBalance());
        assertEquals(WalletTransactionType.RELEASE, walletDAO.transactions.get(0).getType());
        assertEquals(250_000L, walletDAO.transactions.get(0).getAmount());
        assertEquals("reservation_reconcile", walletDAO.transactions.get(0).getReference());
    }

    @Test
    void finishedAuctionsAreCapturedAndOnlyRunningAuctionsRemainReserved() {
        InMemoryAuctionDAO auctionDAO = new InMemoryAuctionDAO();
        InMemoryWalletDAO walletDAO = new InMemoryWalletDAO();
        ServiceTestSupport.EmptyNotificationDAO notificationDAO = new ServiceTestSupport.EmptyNotificationDAO();
        AuctionService auctionService = new AuctionService(auctionDAO, new EmptyAutoBidDAO(), new WalletService(walletDAO), new NotificationService(notificationDAO));

        int bidderId = 9;
        walletDAO.putWallet(new Wallet(1, bidderId, 1_000_000L, 600_000L, 1L, 1L));
        auctionDAO.putAuction(auction(1, 5, bidderId, 150_000.0, AuctionStatus.RUNNING));
        auctionDAO.putAuction(auction(2, 5, bidderId, 200_000.0, AuctionStatus.FINISHED));
        auctionDAO.putAuction(auction(3, 5, bidderId, 100_000.0, AuctionStatus.PAID));
        auctionDAO.putAuction(auction(4, 5, -1, 75_000.0, AuctionStatus.CANCELED));
        auctionDAO.putBid(new BidTransaction(1, 1, bidderId, 150_000.0, 10L));
        auctionDAO.putBid(new BidTransaction(2, 2, bidderId, 200_000.0, 11L));
        auctionDAO.putBid(new BidTransaction(3, 3, bidderId, 100_000.0, 12L));
        auctionDAO.putBid(new BidTransaction(4, 4, bidderId, 75_000.0, 13L));

        auctionService.reconcileReservedBalances();

        Wallet wallet = walletDAO.findWalletByUserId(bidderId);
        assertEquals(800_000L, wallet.getBalance());
        assertEquals(150_000L, wallet.getReservedBalance());
        assertEquals(650_000L, wallet.getAvailableBalance());
        assertEquals(AuctionStatus.PAID, auctionDAO.findAuctionById(2).getStatus());
        assertEquals(WalletTransactionType.CAPTURE, walletDAO.transactions.get(0).getType());
        assertEquals(200_000L, walletDAO.transactions.get(0).getAmount());
    }

    private static AuctionItem auction(int id, int sellerId, int winnerId, double currentBid, AuctionStatus status) {
        return new AuctionItem(
                id,
                "Auction " + id,
                "Test auction",
                10_000.0,
                currentBid,
                1L,
                System.currentTimeMillis() + 60_000L,
                "Test",
                1L,
                1L,
                sellerId,
                winnerId,
                status
        );
    }

    private static final class InMemoryAuctionDAO implements AuctionDAO {
        private final Map<Integer, AuctionItem> auctions = new HashMap<>();
        private final List<BidTransaction> bids = new ArrayList<>();

        void putAuction(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        void putBid(BidTransaction bid) {
            bids.add(bid);
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
        public void updateAuctionState(AuctionItem item) {
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
            return new ArrayList<>(auctions.values());
        }

        @Override
        public List<AuctionItem> findAllAuctionSummaries() {
            return findAllAuctions();
        }

        @Override
        public List<AuctionItem> findAuctionsBySeller(int sellerId) {
            return auctions.values().stream()
                    .filter(item -> item.getSellerId() == sellerId)
                    .toList();
        }

        @Override
        public List<AuctionItem> findStatusRefreshCandidates(long now) {
            return auctions.values().stream()
                    .filter(item -> item.getStatus() == AuctionStatus.RUNNING ||
                            (item.getStatus() == AuctionStatus.OPEN && now >= item.getStartTime()))
                    .toList();
        }

        @Override
        public List<Integer> findAllAuctionIds() {
            return auctions.keySet().stream().sorted().toList();
        }

        @Override
        public List<AuctionItem> findFinishedAuctions() {
            return auctions.values().stream()
                    .filter(item -> item.getStatus() == AuctionStatus.FINISHED)
                    .toList();
        }

        @Override
        public List<AuctionItem> findAuctionsHoldingReservedFunds() {
            return auctions.values().stream()
                    .filter(item -> item.getStatus() == AuctionStatus.RUNNING)
                    .filter(item -> item.getWinnerId() > 0)
                    .filter(item -> item.getCurrentHighestBid() > 0)
                    .toList();
        }

        @Override
        public void saveBid(BidTransaction bid) {
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

        @Override
        public int countAllBids() {
            return bids.size();
        }
    }

    private static final class EmptyAutoBidDAO implements AutoBidDAO {
        @Override
        public void saveAutoBid(AutoBid item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAutoBid(AutoBid item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAutoBid(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAutoBidByAuctionBidder(int auctionId, int bidderId) {
            throw new UnsupportedOperationException();
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
        public List<AutoBid> findAutoBidsByAuction(int auctionId) {
            return List.of();
        }

        @Override
        public List<AutoBid> findAllUserAutoBid(int bidderId) {
            return List.of();
        }
    }

    private static final class InMemoryWalletDAO implements WalletDAO {
        private final Map<Integer, Wallet> walletsByUserId = new HashMap<>();
        private final List<WalletTransaction> transactions = new ArrayList<>();
        private int nextWalletId = 1;

        void putWallet(Wallet wallet) {
            walletsByUserId.put(wallet.getUserId(), wallet);
        }

        @Override
        public int saveWallet(Wallet wallet) throws ValidationException {
            validateWallet(wallet);
            wallet.setId(nextWalletId++);
            walletsByUserId.put(wallet.getUserId(), wallet);
            return wallet.getId();
        }

        @Override
        public void updateWallet(Wallet wallet) throws ValidationException {
            validateWallet(wallet);
            walletsByUserId.put(wallet.getUserId(), wallet);
        }

        @Override
        public Wallet findWalletByUserId(int userId) {
            return walletsByUserId.get(userId);
        }

        @Override
        public int saveTopUpTransaction(TopUpTransaction transaction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateTopUpTransaction(TopUpTransaction transaction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TopUpTransaction findTopUpTransactionById(int transactionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TopUpTransaction> findTopUpTransactionsByUserId(int userId) {
            return List.of();
        }

        @Override
        public void saveWalletTransaction(WalletTransaction transaction) {
            transactions.add(transaction);
        }

        @Override
        public List<WalletTransaction> findWalletTransactionsByUserId(int userId) {
            return transactions.stream()
                    .filter(transaction -> transaction.getUserId() == userId)
                    .toList();
        }

        private void validateWallet(Wallet wallet) throws ValidationException {
            if (wallet.getReservedBalance() < 0) {
                throw new ValidationException("Reserved balance cannot be negative.");
            }
            if (wallet.getReservedBalance() > wallet.getBalance()) {
                throw new ValidationException("Reserved balance cannot exceed the wallet balance.");
            }
        }
    }
}
