package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.Wallet;
import userauth.model.WalletTransaction;
import userauth.model.WalletTransactionType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionAutoPaymentCaptureTest {
    @Test
    void endedRunningAuctionCapturesWinnerFundsAndMarksAuctionPaid() {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        ServiceTestSupport.EmptyNotificationDAO emptyNotificationDAO = new ServiceTestSupport.EmptyNotificationDAO();
        AuctionService auctionService = new AuctionService(
                auctionDAO,
                new ServiceTestSupport.EmptyAutoBidDAO(),
                new WalletService(walletDAO),
                new NotificationService(emptyNotificationDAO)
        );

        int auctionId = 31;
        int bidderId = 7;
        AuctionItem auction = ServiceTestSupport.runningAuction(
                auctionId,
                5,
                100_000.0,
                System.currentTimeMillis() - 1_000L
        );
        auction.setWinnerId(bidderId);
        auction.setCurrentHighestBid(250_000.0);
        auctionDAO.putAuction(auction);
        walletDAO.putWallet(ServiceTestSupport.wallet(1, bidderId, 1_000_000L, 250_000L));

        auctionService.refreshAuctionStatuses();

        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);
        Wallet wallet = walletDAO.findWalletByUserId(bidderId);
        List<WalletTransaction> transactions = walletDAO.transactions();

        assertEquals(AuctionStatus.PAID, storedAuction.getStatus());
        assertEquals(750_000L, wallet.getBalance());
        assertEquals(0L, wallet.getReservedBalance());
        assertEquals(750_000L, wallet.getAvailableBalance());
        assertTrue(transactions.stream().anyMatch(transaction ->
                transaction.getType() == WalletTransactionType.CAPTURE
                        && transaction.getAmount() == 250_000L
        ));
    }

    @Test
    void sellerManualCloseCapturesWinnerFundsWithoutDeletingAuction() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        ServiceTestSupport.EmptyNotificationDAO emptyNotificationDAO = new ServiceTestSupport.EmptyNotificationDAO();
        AuctionService auctionService = new AuctionService(
                auctionDAO,
                new ServiceTestSupport.EmptyAutoBidDAO(),
                new WalletService(walletDAO),
                new NotificationService(emptyNotificationDAO)
        );

        int auctionId = 32;
        int sellerId = 5;
        int bidderId = 8;
        AuctionItem auction = ServiceTestSupport.runningAuction(
                auctionId,
                sellerId,
                100_000.0,
                System.currentTimeMillis() + 60_000L
        );
        auction.setWinnerId(bidderId);
        auction.setCurrentHighestBid(300_000.0);
        auctionDAO.putAuction(auction);
        walletDAO.putWallet(ServiceTestSupport.wallet(1, bidderId, 1_000_000L, 300_000L));

        auctionService.closeAuctionManually(auctionId, sellerId);

        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);
        Wallet wallet = walletDAO.findWalletByUserId(bidderId);
        List<WalletTransaction> transactions = walletDAO.transactions();

        assertEquals(AuctionStatus.PAID, storedAuction.getStatus());
        assertEquals(700_000L, wallet.getBalance());
        assertEquals(0L, wallet.getReservedBalance());
        assertEquals(700_000L, wallet.getAvailableBalance());
        assertTrue(transactions.stream().anyMatch(transaction ->
                transaction.getType() == WalletTransactionType.CAPTURE
                        && transaction.getAmount() == 300_000L
        ));
    }
}
