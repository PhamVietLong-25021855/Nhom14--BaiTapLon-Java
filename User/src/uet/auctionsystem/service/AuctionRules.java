package uet.auctionsystem.service;

// Ghi chu file: File service; chua nghiep vu chinh va phoi hop giua controller, DAO va event.
// Khai bao lop AuctionRules; chua xu ly nghiep vu va cac quy tac chinh cua he thong.
public final class AuctionRules {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho bytes.
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho ms.
    public static final long ANTI_SNIPING_WINDOW_MS = 30_000L;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho extensions.
    public static final int MAX_ANTI_SNIPING_EXTENSIONS = 3;
    // Ham tao: khoi tao doi tuong AuctionRules voi cac phu thuoc can thiet.
    private AuctionRules() {
    }
}
