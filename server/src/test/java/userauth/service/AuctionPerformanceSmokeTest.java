package userauth.service;

import org.junit.jupiter.api.Test;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionPerformanceSmokeTest {
    private static final int CREATE_COUNT = 5_000;
    private static final int SEQUENTIAL_BID_COUNT = 500;
    private static final int CONCURRENT_BIDDERS = 32;
    private static final int ANTI_SNIPING_BID_COUNT = 100;

    @Test
    void measuresCoreAuctionOperationLatencyWithoutDatabase() throws Exception {
        Metric createMetric = measureCreateAuctions();
        Metric sequentialBidMetric = withSuppressedStdout(this::measureSequentialBids);
        ConcurrentBidMetric concurrentBidMetric = withSuppressedStdout(this::measureConcurrentBids);
        Metric antiSnipingMetric = withSuppressedStdout(this::measureAntiSnipingBids);

        System.out.printf(Locale.ROOT, "[PerfSmoke] createAuction: %,d ops, total %,d ms, avg %.3f ms/op.%n",
                createMetric.operations(), createMetric.elapsedMs(), createMetric.averageMs());
        System.out.printf(Locale.ROOT, "[PerfSmoke] placeBid sequential: %,d ops, total %,d ms, avg %.3f ms/op.%n",
                sequentialBidMetric.operations(), sequentialBidMetric.elapsedMs(), sequentialBidMetric.averageMs());
        System.out.printf(Locale.ROOT, "[PerfSmoke] placeBid concurrent same auction: %,d attempts, %,d accepted, total %,d ms, avg %.3f ms/attempt.%n",
                concurrentBidMetric.attempts(), concurrentBidMetric.accepted(), concurrentBidMetric.elapsedMs(), concurrentBidMetric.averageMs());
        System.out.printf(Locale.ROOT, "[PerfSmoke] anti-sniping final-window bids: %,d ops, total %,d ms, avg %.3f ms/op.%n",
                antiSnipingMetric.operations(), antiSnipingMetric.elapsedMs(), antiSnipingMetric.averageMs());

        assertEquals(CREATE_COUNT, createMetric.operations());
        assertEquals(SEQUENTIAL_BID_COUNT, sequentialBidMetric.operations());
        assertEquals(ANTI_SNIPING_BID_COUNT, antiSnipingMetric.operations());
        assertTrue(concurrentBidMetric.accepted() > 0, "At least one concurrent bid should be accepted.");
    }

    private Metric measureCreateAuctions() throws Exception {
        ServiceFixture fixture = fixture();
        long now = System.currentTimeMillis();
        long startedAt = System.nanoTime();
        for (int index = 0; index < CREATE_COUNT; index++) {
            fixture.auctionService().createAuction(
                    "Product " + index,
                    "Benchmark product",
                    100_000.0 + index,
                    now + 60_000L,
                    now + 3_600_000L,
                    "Benchmark",
                    null,
                    null,
                    5
            );
        }
        return new Metric(CREATE_COUNT, elapsedMs(startedAt));
    }

    private Metric measureSequentialBids() throws Exception {
        ServiceFixture fixture = fixture();
        int auctionId = 1;
        fixture.auctionDAO().putAuction(ServiceTestSupport.runningAuction(
                auctionId,
                5,
                100_000.0,
                System.currentTimeMillis() + 3_600_000L
        ));
        for (int index = 0; index < SEQUENTIAL_BID_COUNT; index++) {
            int bidderId = 1_000 + index;
            fixture.walletDAO().putWallet(ServiceTestSupport.wallet(bidderId, bidderId, 10_000_000L, 0L));
        }

        long startedAt = System.nanoTime();
        for (int index = 0; index < SEQUENTIAL_BID_COUNT; index++) {
            fixture.auctionService().placeBid(auctionId, 1_000 + index, 101_000.0 + index);
        }
        return new Metric(SEQUENTIAL_BID_COUNT, elapsedMs(startedAt));
    }

    private ConcurrentBidMetric measureConcurrentBids() throws Exception {
        ServiceFixture fixture = fixture();
        int auctionId = 2;
        fixture.auctionDAO().putAuction(ServiceTestSupport.runningAuction(
                auctionId,
                5,
                100_000.0,
                System.currentTimeMillis() + 3_600_000L
        ));
        for (int index = 0; index < CONCURRENT_BIDDERS; index++) {
            int bidderId = 2_000 + index;
            fixture.walletDAO().putWallet(ServiceTestSupport.wallet(bidderId, bidderId, 10_000_000L, 0L));
        }

        CountDownLatch ready = new CountDownLatch(CONCURRENT_BIDDERS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_BIDDERS);
        List<Callable<Boolean>> calls = new ArrayList<>();
        for (int index = 0; index < CONCURRENT_BIDDERS; index++) {
            int bidderId = 2_000 + index;
            double amount = 101_000.0 + (index * 1_000.0);
            calls.add(() -> {
                ready.countDown();
                start.await();
                try {
                    fixture.auctionService().placeBid(auctionId, bidderId, amount);
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> call : calls) {
            futures.add(executor.submit(call));
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        long startedAt = System.nanoTime();
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        long accepted = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                accepted++;
            }
        }
        return new ConcurrentBidMetric(CONCURRENT_BIDDERS, accepted, elapsedMs(startedAt));
    }

    private Metric measureAntiSnipingBids() throws Exception {
        ServiceFixture fixture = fixture();
        for (int index = 0; index < ANTI_SNIPING_BID_COUNT; index++) {
            int auctionId = 10_000 + index;
            int bidderId = 30_000 + index;
            fixture.auctionDAO().putAuction(ServiceTestSupport.runningAuction(
                    auctionId,
                    5,
                    100_000.0,
                    System.currentTimeMillis() + 5_000L
            ));
            fixture.walletDAO().putWallet(ServiceTestSupport.wallet(bidderId, bidderId, 10_000_000L, 0L));
        }

        long startedAt = System.nanoTime();
        for (int index = 0; index < ANTI_SNIPING_BID_COUNT; index++) {
            fixture.auctionService().placeBid(10_000 + index, 30_000 + index, 150_000.0);
        }
        return new Metric(ANTI_SNIPING_BID_COUNT, elapsedMs(startedAt));
    }

    private ServiceFixture fixture() {
        ServiceTestSupport.InMemoryAuctionDAO auctionDAO = new ServiceTestSupport.InMemoryAuctionDAO();
        ServiceTestSupport.InMemoryWalletDAO walletDAO = new ServiceTestSupport.InMemoryWalletDAO();
        AuctionService auctionService = new AuctionService(
                auctionDAO,
                new ServiceTestSupport.EmptyAutoBidDAO(),
                new WalletService(walletDAO),
                new NotificationService(new ServiceTestSupport.EmptyNotificationDAO())
        );
        return new ServiceFixture(auctionDAO, walletDAO, auctionService);
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private <T> T withSuppressedStdout(ThrowingSupplier<T> supplier) throws Exception {
        PrintStream originalOut = System.out;
        try (PrintStream silent = new PrintStream(OutputStream.nullOutputStream())) {
            System.setOut(silent);
            return supplier.get();
        } finally {
            System.setOut(originalOut);
        }
    }

    private record ServiceFixture(
            ServiceTestSupport.InMemoryAuctionDAO auctionDAO,
            ServiceTestSupport.InMemoryWalletDAO walletDAO,
            AuctionService auctionService
    ) {
    }

    private record Metric(int operations, long elapsedMs) {
        double averageMs() {
            return operations == 0 ? 0D : elapsedMs / (double) operations;
        }
    }

    private record ConcurrentBidMetric(int attempts, long accepted, long elapsedMs) {
        double averageMs() {
            return attempts == 0 ? 0D : elapsedMs / (double) attempts;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
