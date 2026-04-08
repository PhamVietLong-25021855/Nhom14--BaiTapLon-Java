package userauth.service;

import userauth.dao.AuctionDAO;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

import java.util.List;

public class AuctionScheduler {
    private final AuctionService auctionService;
    private Thread schedulerThread;
    private volatile boolean running;

    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
        this.running = false;
    }

    public void start() {
        running = true;
        schedulerThread = new Thread(() -> {
            while (running) {
                try {
                    auctionService.refreshAuctionStatuses();
                    Thread.sleep(1000); // Check every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Lỗi trong AuctionScheduler:" + e.getMessage());
                }
            }
        });
        schedulerThread.setDaemon(true); // Don't prevent JVM shutdown
        schedulerThread.start();
    }

    public void stop() {
        running = false;
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
    }
}
