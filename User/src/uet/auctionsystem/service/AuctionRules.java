package uet.auctionsystem.service;

// Ghi chu file: File service; chua nghiep vu chinh va phoi hop giua controller, DAO va event.
// Khai bao lop AuctionRules; chua xu ly nghiep vu va cac quy tac chinh cua he thong.
public final class AuctionRules {
    // Sá»‘ nhá»‹p Ä‘áº¿m mÃ  admin dÃ¹ng Ä‘á»ƒ Ä‘Ã³ng sá»›m phiÃªn Ä‘áº¥u giÃ¡.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho counts.
    public static final int ADMIN_EARLY_CLOSE_COUNTS = 3;
    // Giá»›i háº¡n tá»‘i Ä‘a cho áº£nh seller upload lÃªn DB.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho bytes.
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    // Náº¿u bid rÆ¡i vÃ o khoáº£ng thá»i gian nÃ y trÆ°á»›c lÃºc káº¿t thÃºc thÃ¬ sáº½ Ä‘Æ°á»£c gia háº¡n.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho ms.
    public static final long ANTI_SNIPING_WINDOW_MS = 30_000L;
    // Giá»›i háº¡n sá»‘ láº§n Ä‘Æ°á»£c gia háº¡n Ä‘á»ƒ trÃ¡nh kÃ©o dÃ i vÃ´ háº¡n.
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho extensions.
    public static final int MAX_ANTI_SNIPING_EXTENSIONS = 3;
    // Ham tao: khoi tao doi tuong AuctionRules voi cac phu thuoc can thiet.
    private AuctionRules() {
    }
}
