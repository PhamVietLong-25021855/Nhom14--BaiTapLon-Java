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
import userauth.model.BidTransaction;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {

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

    private static final class InMemoryAuctionDAO implements AuctionDAO {
        private final Map<Integer, AuctionItem> auctions = new HashMap<>();
        private final List<BidTransaction> bids = new ArrayList<>();
        private int nextAuctionId = 1;
        private int nextBidId = 1;

        @Override
        public void saveAuction(AuctionItem item) {
            if (item.getId() <= 0) {
                item.setId(nextAuctionId++);
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
                bid.setId(nextBidId++);
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
            return new AuctionItem(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getStartPrice(),
                    item.getCurrentHighestBid(),
                    item.getStartTime(),
                    item.getEndTime(),
                    item.getCategory(),
                    item.getImageSource(),
                    item.getCreatedAt(),
                    item.getUpdatedAt(),
                    item.getSellerId(),
                    item.getWinnerId(),
                    item.getStatus()
            );
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
        private final Map<Integer, Wallet> wallets = new HashMap<>();
        private int nextWalletId = 1;

        void putWallet(int userId, double balance) {
            Wallet wallet = new Wallet(nextWalletId++, userId, balance, System.currentTimeMillis(), System.currentTimeMillis());
            wallets.put(userId, copyWallet(wallet));
        }

        @Override
        public int saveWallet(Wallet wallet) {
            if (wallet.getId() <= 0) {
                wallet.setId(nextWalletId++);
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
}
