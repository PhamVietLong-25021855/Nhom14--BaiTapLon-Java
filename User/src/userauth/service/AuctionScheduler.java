package userauth.service;

// Ghi chu file: File service; chua nghiep vu chinh va phoi hop giua controller, DAO va event.
// Khai bao lop AuctionScheduler; chua xu ly nghiep vu va cac quy tac chinh cua he thong.
public class AuctionScheduler {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho ms.
    private static final long REFRESH_INTERVAL_MS = 1000L;
    // Thuoc tinh: giu tham chieu den AuctionService de phoi hop xu ly.
    private final AuctionService auctionService;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho scheduler thread.
    private Thread schedulerThread;
    private volatile boolean running;
    // Ham tao: khoi tao doi tuong AuctionScheduler voi cac phu thuoc can thiet.
    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh start.
    public void start() {
        if (running) {
            return;
        }

        running = true;
        schedulerThread = new Thread(this::runLoop, "auction-scheduler");
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac stop.
    public void stop() {
        running = false;
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
    }
    // Phuong thuc: thuc hien chuc nang run loop trong lop AuctionScheduler.
    private void runLoop() {
        while (running) {
            try {
                auctionService.refreshAuctionStatuses();
                Thread.sleep(REFRESH_INTERVAL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                System.err.println("AuctionScheduler error: " + ex.getMessage());
            }
        }
    }
}
