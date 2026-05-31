package userauth.service;

import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.dao.NotificationDAO;
import userauth.dao.WalletDAO;
import userauth.exception.ValidationException;
import userauth.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class ServiceTestSupport {
    private ServiceTestSupport() {
    }

    static AuctionItem runningAuction(int id, int sellerId, double startPrice, long endTime) {
        long now = System.currentTimeMillis();
        return new AuctionItem(
                id,
                "Auction " + id,
                "Test auction",
                startPrice,
                startPrice,
                now - 60_000L,
                endTime,
                "Test",
                now,
                now,
                sellerId,
                -1,
                AuctionStatus.RUNNING
        );
    }

    static Wallet wallet(int id, int userId, long balance, long reservedBalance) {
        return new Wallet(id, userId, balance, reservedBalance, 1L, 1L);
    }

    static final class InMemoryAuctionDAO implements AuctionDAO {
        private final Map<Integer, AuctionItem> auctions = new HashMap<>();
        private final List<BidTransaction> bids = new ArrayList<>();
        private final AtomicInteger nextAuctionId = new AtomicInteger(1);
        private final AtomicInteger nextBidId = new AtomicInteger(1);

        synchronized void putAuction(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        @Override
        public synchronized void saveAuction(AuctionItem item) {
            if (item.getId() <= 0) {
                item.setId(nextAuctionId.getAndIncrement());
            }
            auctions.put(item.getId(), item);
        }

        @Override
        public synchronized void updateAuction(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        @Override
        public synchronized void updateAuctionState(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        @Override
        public synchronized void deleteAuction(int id) {
            auctions.remove(id);
        }

        @Override
        public synchronized AuctionItem findAuctionById(int id) {
            return auctions.get(id);
        }

        @Override
        public synchronized List<AuctionItem> findAllAuctions() {
            return new ArrayList<>(auctions.values());
        }

        @Override
        public synchronized List<AuctionItem> findAllAuctionSummaries() {
            return findAllAuctions();
        }

        @Override
        public synchronized List<AuctionItem> findAuctionsBySeller(int sellerId) {
            return auctions.values().stream()
                    .filter(item -> item.getSellerId() == sellerId)
                    .toList();
        }

        @Override
        public synchronized List<AuctionItem> findStatusRefreshCandidates(long now) {
            return auctions.values().stream()
                    .filter(item -> item.getStatus() == AuctionStatus.RUNNING ||
                            (item.getStatus() == AuctionStatus.OPEN && now >= item.getStartTime()))
                    .toList();
        }

        @Override
        public synchronized List<Integer> findAllAuctionIds() {
            return auctions.keySet().stream().sorted().toList();
        }

        @Override
        public synchronized List<AuctionItem> findFinishedAuctions() {
            return auctions.values().stream()
                    .filter(item -> item.getStatus() == AuctionStatus.FINISHED)
                    .toList();
        }

        @Override
        public synchronized List<AuctionItem> findAuctionsHoldingReservedFunds() {
            return auctions.values().stream()
                    .filter(item -> item.getStatus() == AuctionStatus.RUNNING)
                    .filter(item -> item.getWinnerId() > 0)
                    .filter(item -> item.getCurrentHighestBid() > 0)
                    .toList();
        }

        @Override
        public synchronized void saveBid(BidTransaction bid) {
            if (bid.getId() <= 0) {
                bid.setId(nextBidId.getAndIncrement());
            }
            bids.add(bid);
        }

        @Override
        public synchronized List<BidTransaction> findAllBids() {
            return orderedBids(bids);
        }

        @Override
        public synchronized List<BidTransaction> findBidsByAuction(int auctionId) {
            return orderedBids(bids.stream()
                    .filter(bid -> bid.getAuctionId() == auctionId)
                    .toList());
        }

        @Override
        public synchronized int countAllBids() {
            return bids.size();
        }

        private List<BidTransaction> orderedBids(List<BidTransaction> source) {
            return source.stream()
                    .sorted(Comparator
                            .comparingLong(BidTransaction::getTimestamp)
                            .thenComparingInt(BidTransaction::getId))
                    .toList();
        }
    }

    static final class EmptyAutoBidDAO implements AutoBidDAO {
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

    static final class InMemoryWalletDAO implements WalletDAO {
        private final Map<Integer, Wallet> walletsByUserId = new HashMap<>();
        private final List<WalletTransaction> transactions = new ArrayList<>();
        private int nextWalletId = 1;

        synchronized void putWallet(Wallet wallet) {
            walletsByUserId.put(wallet.getUserId(), wallet);
        }

        synchronized List<WalletTransaction> transactions() {
            return new ArrayList<>(transactions);
        }

        @Override
        public synchronized int saveWallet(Wallet wallet) throws ValidationException {
            validateWallet(wallet);
            wallet.setId(nextWalletId++);
            walletsByUserId.put(wallet.getUserId(), wallet);
            return wallet.getId();
        }

        @Override
        public synchronized void updateWallet(Wallet wallet) throws ValidationException {
            validateWallet(wallet);
            walletsByUserId.put(wallet.getUserId(), wallet);
        }

        @Override
        public synchronized Wallet findWalletByUserId(int userId) {
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
        public synchronized void saveWalletTransaction(WalletTransaction transaction) {
            transactions.add(transaction);
        }

        @Override
        public synchronized List<WalletTransaction> findWalletTransactionsByUserId(int userId) {
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

    static final class EmptyNotificationDAO implements NotificationDAO {

        @Override
        public void saveNotification(Notification item) {

        }

        @Override
        public List<Notification> findNotificationToUser(int user_id) {
            return List.of();
        }
    }
}
