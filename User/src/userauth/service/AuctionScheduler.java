package userauth.service;

public class AuctionScheduler {
    private static final long REFRESH_INTERVAL_MS = 1000L;

    private final AuctionService auctionService;
    private Thread schedulerThread;
    private volatile boolean running;

    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        schedulerThread = new Thread(this::runLoop, "auction-scheduler");
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    public void stop() {
        running = false;
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
    }

    private void runLoop() {
        while (running) {
            try {
                auctionService.refreshAuctionStatuses();
                Thread.sleep(REFRESH_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                System.err.println("Loi trong AuctionScheduler: " + ex.getMessage());
            }
        }
    }
}
