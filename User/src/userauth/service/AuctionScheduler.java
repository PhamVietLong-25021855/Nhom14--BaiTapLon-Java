package userauth.service;

import userauth.dao.AuctionDAO;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

import java.util.List;

public class AuctionScheduler {
    private final AuctionDAO auctionDAO;
    private Thread schedulerThread;
    private volatile boolean running;

    public AuctionScheduler(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
        this.running = false;
    }

    public void start() {
        running = true;
        schedulerThread = new Thread(() -> {
            while (running) {
                try {
                    long now = System.currentTimeMillis();
                    List<AuctionItem> auctions = auctionDAO.findAllAuctions();
                    boolean changed = false;

                    for (AuctionItem item : auctions) {
                        AuctionStatus currentStatus = item.getStatus();

                        // Trạng thái OPEN -> RUNNING nếu tới giờ
                        if (currentStatus == AuctionStatus.OPEN && now >= item.getStartTime() && now < item.getEndTime()) {
                            item.setStatus(AuctionStatus.RUNNING);
                            item.setUpdatedAt(now);
                            changed = true;
                        }

                        // Trạng thái RUNNING -> FINISHED nếu hết giờ
                        if (currentStatus == AuctionStatus.RUNNING && now >= item.getEndTime()) {
                            item.setStatus(AuctionStatus.FINISHED);
                            item.setUpdatedAt(now);
                            changed = true;
                        }
                    }

                    if (changed) {
                        for (AuctionItem item : auctions) {
                            auctionDAO.updateAuction(item); // Note: Could be optimized, but this writes to file
                        }
                    }

                    Thread.sleep(1000); // Check every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Lỗi trong AuctionScheduler: " + e.getMessage());
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
