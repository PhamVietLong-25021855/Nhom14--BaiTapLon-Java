package userauth.service;

// File note: Gom các hằng số nghiệp vụ liên quan đến auction để UI/service dùng chung.
// Gom cÃ¡c ngÆ°á»¡ng nghiá»‡p vá»¥ Ä‘á»ƒ service/UI dÃ¹ng chung, trÃ¡nh hard-code ráº£i rÃ¡c.
public final class AuctionRules {
    // Sá»‘ nhá»‹p Ä‘áº¿m mÃ  admin dÃ¹ng Ä‘á»ƒ Ä‘Ã³ng sá»›m phiÃªn Ä‘áº¥u giÃ¡.
    public static final int ADMIN_EARLY_CLOSE_COUNTS = 3;
    // Giá»›i háº¡n tá»‘i Ä‘a cho áº£nh seller upload lÃªn DB.
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    // Náº¿u bid rÆ¡i vÃ o khoáº£ng thá»i gian nÃ y trÆ°á»›c lÃºc káº¿t thÃºc thÃ¬ sáº½ Ä‘Æ°á»£c gia háº¡n.
    public static final long ANTI_SNIPING_WINDOW_MS = 30_000L;
    // Giá»›i háº¡n sá»‘ láº§n Ä‘Æ°á»£c gia háº¡n Ä‘á»ƒ trÃ¡nh kÃ©o dÃ i vÃ´ háº¡n.
    public static final int MAX_ANTI_SNIPING_EXTENSIONS = 3;

    private AuctionRules() {
    }
}

