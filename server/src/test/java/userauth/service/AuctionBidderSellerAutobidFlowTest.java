package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.controller.AuctionController;
import userauth.controller.AutobidController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.Wallet;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionBidderSellerAutobidFlowTest {
    @Test
    void sellerCreatesAuctionAndBiddersCompeteWithAutobidAndWalletReservations() {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryAutoBidDAO autoBidDAO = new ServiceTestSupport.InMemoryAutoBidDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        ServiceTestSupport.EmptyNotificationDAO notificationDAO = new ServiceTestSupport.EmptyNotificationDAO();

        AuctionService auctionService = new AuctionService(
                auctionDAO,
                autoBidDAO,
                new WalletService(walletDAO),
                new NotificationService(notificationDAO)
        );
        AuctionController auctionController = new AuctionController(auctionService);
        AutobidController autobidController = new AutobidController(new AutobidService(autoBidDAO, auctionService));

        int sellerId = 10;
        int autoBidderOne = 21;
        int autoBidderTwo = 22;
        int manualBidder = 23;
        walletDAO.putWallet(ServiceTestSupport.wallet(1, autoBidderOne, 1_000_000L, 0L));
        walletDAO.putWallet(ServiceTestSupport.wallet(2, autoBidderTwo, 1_000_000L, 0L));
        walletDAO.putWallet(ServiceTestSupport.wallet(3, manualBidder, 1_000_000L, 0L));

        long now = System.currentTimeMillis();
        String createResult = auctionController.createAuction(
                "Flow Smoke Auction",
                "Seller to bidder smoke test",
                100_000.0,
                now - 1_000L,
                now + 120_000L,
                "Smoke",
                null,
                null,
                1_000.0,
                sellerId
        );
        assertEquals("SUCCESS", createResult);

        AuctionItem auction = auctionController.getAllAuctions().getFirst();
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        int auctionId = auction.getId();

        assertEquals("SUCCESS", autobidController.createAutobid(autoBidderOne, auctionId, 180_000.0, 20_000.0));
        assertEquals("SUCCESS", autobidController.createAutobid(autoBidderTwo, auctionId, 220_000.0, 20_000.0));

        AutoBid autoBidOne = autoBidDAO.findAutoBidByAuctionBidder(auctionId, autoBidderOne);
        assertNotNull(autoBidOne);
        assertEquals("SUCCESS", autobidController.createAutobid(autoBidderOne, auctionId, 190_000.0, 20_000.0));
        assertEquals(1, autobidController.getAutobidByBidder(autoBidderOne).stream()
                .filter(item -> item.getAuctionId() == auctionId)
                .count());

        assertEquals("SUCCESS", auctionController.placeBid(auctionId, manualBidder, 215_000.0));

        AuctionItem updatedAuction = auctionDAO.findAuctionById(auctionId);
        assertEquals(autoBidderTwo, updatedAuction.getWinnerId());
        assertEquals(220_000.0, updatedAuction.getCurrentHighestBid());

        Wallet autoBidderTwoWallet = walletDAO.findWalletByUserId(autoBidderTwo);
        Wallet manualBidderWallet = walletDAO.findWalletByUserId(manualBidder);
        assertEquals(220_000L, autoBidderTwoWallet.getReservedBalance());
        assertEquals(0L, manualBidderWallet.getReservedBalance());
        assertTrue(auctionController.getBidsForAuction(auctionId).size() >= 4);

        Map<Integer, Integer> bidCounts = auctionController.getBidCounts();
        assertEquals(auctionController.getBidsForAuction(auctionId).size(), bidCounts.get(auctionId));

        assertEquals("SUCCESS", autobidController.deleteAutoBid(autoBidderOne, autoBidOne.getId()));
        List<AutoBid> remainingAutoBids = autobidController.getAutobidByBidder(autoBidderOne);
        assertTrue(remainingAutoBids.stream().noneMatch(item -> item.getAuctionId() == auctionId));

        assertEquals("SUCCESS", auctionController.closeAuction(auctionId, sellerId));
        AuctionItem closedAuction = auctionDAO.findAuctionById(auctionId);
        assertEquals(AuctionStatus.PAID, closedAuction.getStatus());
        assertEquals(780_000L, walletDAO.findWalletByUserId(autoBidderTwo).getBalance());
        assertEquals(0L, walletDAO.findWalletByUserId(autoBidderTwo).getReservedBalance());
    }
}
