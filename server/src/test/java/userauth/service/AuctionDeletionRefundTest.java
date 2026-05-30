package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;
import userauth.model.Wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuctionDeletionRefundTest {
    @Test
    void sellerDeleteAuctionWithBidsCancelsAuctionAndReleasesWinnerReservation() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        AuctionService auctionService = auctionService(auctionDAO, walletDAO);

        int auctionId = 501;
        int sellerId = 5;
        int bidderId = 9;
        AuctionItem auction = ServiceTestSupport.runningAuction(auctionId, sellerId, 100_000.0, System.currentTimeMillis() + 60_000L);
        auction.setWinnerId(bidderId);
        auction.setCurrentHighestBid(250_000.0);
        auctionDAO.putAuction(auction);
        auctionDAO.saveBid(new BidTransaction(0, auctionId, bidderId, 250_000.0, System.currentTimeMillis(), "ACCEPTED"));
        walletDAO.putWallet(ServiceTestSupport.wallet(1, bidderId, 1_000_000L, 250_000L));

        auctionService.deleteAuction(auctionId, sellerId);

        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);
        Wallet wallet = walletDAO.findWalletByUserId(bidderId);
        assertEquals(AuctionStatus.CANCELED, storedAuction.getStatus());
        assertEquals(-1, storedAuction.getWinnerId());
        assertEquals(0L, wallet.getReservedBalance());
        assertEquals(1_000_000L, wallet.getAvailableBalance());
    }

    @Test
    void adminDeleteAuctionWithBidsCancelsAuctionAndReleasesWinnerReservation() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        AuctionService auctionService = auctionService(auctionDAO, walletDAO);

        int auctionId = 502;
        int sellerId = 6;
        int bidderId = 10;
        AuctionItem auction = ServiceTestSupport.runningAuction(auctionId, sellerId, 100_000.0, System.currentTimeMillis() + 60_000L);
        auction.setWinnerId(bidderId);
        auction.setCurrentHighestBid(300_000.0);
        auctionDAO.putAuction(auction);
        auctionDAO.saveBid(new BidTransaction(0, auctionId, bidderId, 300_000.0, System.currentTimeMillis(), "ACCEPTED"));
        walletDAO.putWallet(ServiceTestSupport.wallet(1, bidderId, 1_000_000L, 300_000L));

        auctionService.deleteAuctionAsAdmin(auctionId);

        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);
        Wallet wallet = walletDAO.findWalletByUserId(bidderId);
        assertEquals(AuctionStatus.CANCELED, storedAuction.getStatus());
        assertEquals(-1, storedAuction.getWinnerId());
        assertEquals(0L, wallet.getReservedBalance());
        assertEquals(1_000_000L, wallet.getAvailableBalance());
    }

    @Test
    void adminDeleteAuctionWithoutBidsRemovesAuctionRecord() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        AuctionService auctionService = auctionService(auctionDAO, new ServiceTestSupport.InMemoryWalletDAO());

        int auctionId = 503;
        auctionDAO.putAuction(ServiceTestSupport.runningAuction(auctionId, 7, 100_000.0, System.currentTimeMillis() + 60_000L));

        auctionService.deleteAuctionAsAdmin(auctionId);

        assertNull(auctionDAO.findAuctionById(auctionId));
    }

    private AuctionService auctionService(ServiceTestSupport.InMemoryAuctionDAO auctionDAO, ServiceTestSupport.InMemoryWalletDAO walletDAO) {
        return new AuctionService(
                auctionDAO,
                new ServiceTestSupport.EmptyAutoBidDAO(),
                new WalletService(walletDAO),
                new NotificationService(new ServiceTestSupport.EmptyNotificationDAO())
        );
    }
}
