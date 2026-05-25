package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.common.AuctionRules;
import userauth.model.AuctionItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionAntiSnipingTest {
    @Test
    void bidInsideFinalWindowExtendsAuctionEndTime() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        ServiceTestSupport.EmptyNotificationDAO notificationDAO = new ServiceTestSupport.EmptyNotificationDAO();
        AuctionService auctionService = auctionService(auctionDAO, walletDAO, notificationDAO);

        int auctionId = 201;
        long beforeBid = System.currentTimeMillis();
        long originalEndTime = beforeBid + 5_000L;
        auctionDAO.putAuction(ServiceTestSupport.runningAuction(auctionId, 5, 100_000.0, originalEndTime));
        walletDAO.putWallet(ServiceTestSupport.wallet(1, 21, 1_000_000L, 0L));

        auctionService.placeBid(auctionId, 21, 150_000.0);

        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);
        assertEquals(1, storedAuction.getAntiSnipingExtensionCount());
        assertTrue(storedAuction.getEndTime() > originalEndTime);
        assertTrue(storedAuction.getEndTime() >= beforeBid + AuctionRules.ANTI_SNIPING_WINDOW_MS);
    }

    @Test
    void bidOutsideFinalWindowDoesNotExtendAuctionEndTime() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        ServiceTestSupport.EmptyNotificationDAO notificationDAO = new ServiceTestSupport.EmptyNotificationDAO();
        AuctionService auctionService = auctionService(auctionDAO, walletDAO, notificationDAO);

        int auctionId = 202;
        long originalEndTime = System.currentTimeMillis() + AuctionRules.ANTI_SNIPING_WINDOW_MS + 20_000L;
        auctionDAO.putAuction(ServiceTestSupport.runningAuction(auctionId, 5, 100_000.0, originalEndTime));
        walletDAO.putWallet(ServiceTestSupport.wallet(1, 22, 1_000_000L, 0L));

        auctionService.placeBid(auctionId, 22, 150_000.0);

        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);
        assertEquals(0, storedAuction.getAntiSnipingExtensionCount());
        assertEquals(originalEndTime, storedAuction.getEndTime());
    }

    @Test
    void antiSnipingExtensionsStopAtConfiguredLimit() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        ServiceTestSupport.EmptyNotificationDAO notificationDAO = new ServiceTestSupport.EmptyNotificationDAO();
        AuctionService auctionService = auctionService(auctionDAO, walletDAO,notificationDAO);

        int auctionId = 203;
        auctionDAO.putAuction(ServiceTestSupport.runningAuction(
                auctionId,
                5,
                100_000.0,
                System.currentTimeMillis() + 5_000L
        ));
        for (int bidderId = 31; bidderId <= 34; bidderId++) {
            walletDAO.putWallet(ServiceTestSupport.wallet(bidderId, bidderId, 1_000_000L, 0L));
        }

        for (int i = 0; i < AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS; i++) {
            AuctionItem auction = auctionDAO.findAuctionById(auctionId);
            auction.setEndTime(System.currentTimeMillis() + 5_000L);
            auctionDAO.updateAuction(auction);
            auctionService.placeBid(auctionId, 31 + i, 150_000.0 + (i * 25_000.0));
        }

        AuctionItem auctionBeforeLimitBid = auctionDAO.findAuctionById(auctionId);
        auctionBeforeLimitBid.setEndTime(System.currentTimeMillis() + 5_000L);
        auctionDAO.updateAuction(auctionBeforeLimitBid);
        long endTimeBeforeLimitBid = auctionBeforeLimitBid.getEndTime();

        auctionService.placeBid(auctionId, 34, 250_000.0);

        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);
        assertEquals(AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS, storedAuction.getAntiSnipingExtensionCount());
        assertEquals(endTimeBeforeLimitBid, storedAuction.getEndTime());
    }

    private AuctionService auctionService(
            ServiceTestSupport.InMemoryAuctionDAO auctionDAO,
            ServiceTestSupport.InMemoryWalletDAO walletDAO,
            ServiceTestSupport.EmptyNotificationDAO emptyNotificationDAO
    ) {
        return new AuctionService(
                auctionDAO,
                new ServiceTestSupport.EmptyAutoBidDAO(),
                new WalletService(walletDAO),
                new NotificationService(emptyNotificationDAO)
        );
    }
}
