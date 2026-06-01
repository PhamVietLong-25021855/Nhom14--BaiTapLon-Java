package userauth.common;

public final class AuctionRules {
    public static final int ADMIN_EARLY_CLOSE_COUNTS = 3;
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    public static final long ANTI_SNIPING_WINDOW_MS = 30_000L;
    public static final int MAX_ANTI_SNIPING_EXTENSIONS = 3;
    public static final double MIN_BID_STEP_PERCENT = 0.01d;
    public static final double MAX_BID_STEP_PERCENT = 0.10d;

    private AuctionRules() {
    }
}
