package userauth.service;

public final class AuctionRules {
    public static final int ADMIN_EARLY_CLOSE_COUNTS = 3;
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    public static final long ANTI_SNIPING_WINDOW_MS = 30_000L;
    public static final int MAX_ANTI_SNIPING_EXTENSIONS = 3;

    private AuctionRules() {
    }
}
