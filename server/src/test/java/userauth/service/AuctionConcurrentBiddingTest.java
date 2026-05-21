package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.model.Wallet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionConcurrentBiddingTest {
    @Test
    void concurrentBidsOnSameAuctionKeepSingleHighestWinnerAndConsistentWalletReservations() throws Exception {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        AuctionService auctionService = new AuctionService(
                auctionDAO,
                new ServiceTestSupport.EmptyAutoBidDAO(),
                new WalletService(walletDAO)
        );

        int auctionId = 101;
        long endTime = System.currentTimeMillis() + 120_000L;
        auctionDAO.putAuction(ServiceTestSupport.runningAuction(auctionId, 5, 100_000.0, endTime));

        int[] bidderIds = {11, 12, 13, 14, 15};
        long[] bidAmounts = {130_000L, 160_000L, 120_000L, 190_000L, 175_000L};
        for (int bidderId : bidderIds) {
            walletDAO.putWallet(ServiceTestSupport.wallet(bidderId, bidderId, 1_000_000L, 0L));
        }

        CountDownLatch ready = new CountDownLatch(bidderIds.length);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(bidderIds.length);
        List<Future<BidResult>> futures = new ArrayList<>();
        for (int i = 0; i < bidderIds.length; i++) {
            int bidderId = bidderIds[i];
            long amount = bidAmounts[i];
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    auctionService.placeBid(auctionId, bidderId, amount);
                    return new BidResult(bidderId, amount, true);
                } catch (Exception ex) {
                    return new BidResult(bidderId, amount, false);
                }
            }));
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        List<BidResult> results = new ArrayList<>();
        for (Future<BidResult> future : futures) {
            results.add(future.get());
        }
        long successCount = results.stream().filter(BidResult::success).count();
        assertTrue(successCount > 0);

        List<BidTransaction> acceptedBids = auctionDAO.findBidsByAuction(auctionId);
        assertEquals(successCount, acceptedBids.size());

        BidTransaction highestAcceptedBid = acceptedBids.stream()
                .max(Comparator.comparingDouble(BidTransaction::getAmount))
                .orElseThrow();
        AuctionItem storedAuction = auctionDAO.findAuctionById(auctionId);

        assertEquals(highestAcceptedBid.getAmount(), storedAuction.getCurrentHighestBid());
        assertEquals(highestAcceptedBid.getBidderId(), storedAuction.getWinnerId());
        assertEquals(1, acceptedBids.stream()
                .filter(bid -> Double.compare(bid.getAmount(), storedAuction.getCurrentHighestBid()) == 0)
                .count());

        for (int bidderId : bidderIds) {
            Wallet wallet = walletDAO.findWalletByUserId(bidderId);
            long expectedReserved = bidderId == storedAuction.getWinnerId()
                    ? (long) storedAuction.getCurrentHighestBid()
                    : 0L;
            assertEquals(expectedReserved, wallet.getReservedBalance());
        }
    }

    private record BidResult(int bidderId, long amount, boolean success) {
    }
}
