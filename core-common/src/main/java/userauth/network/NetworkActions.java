package userauth.network;

public final class NetworkActions {
    private NetworkActions() {}

    public static final String PING = "PING";

    public static final String AUTH_REGISTER = "AUTH_REGISTER";
    public static final String AUTH_LOGIN = "AUTH_LOGIN";
    public static final String AUTH_ALL_USERS = "AUTH_ALL_USERS";
    public static final String AUTH_CHANGE_PASSWORD = "AUTH_CHANGE_PASSWORD";
    public static final String AUTH_UPDATE_PROFILE = "AUTH_UPDATE_PROFILE";
    public static final String AUTH_TOGGLE_STATUS = "AUTH_TOGGLE_STATUS";
    public static final String AUTH_DELETE_USER = "AUTH_DELETE_USER";

    public static final String AUCTION_CREATE = "AUCTION_CREATE";
    public static final String AUCTION_UPDATE = "AUCTION_UPDATE";
    public static final String AUCTION_DELETE = "AUCTION_DELETE";
    public static final String AUCTION_BY_SELLER = "AUCTION_BY_SELLER";
    public static final String AUCTION_ALL = "AUCTION_ALL";
    public static final String AUCTION_BIDS = "AUCTION_BIDS";
    public static final String AUCTION_ALL_BIDS = "AUCTION_ALL_BIDS";
    public static final String AUCTION_PLACE_BID = "AUCTION_PLACE_BID";
    public static final String AUCTION_CLOSE = "AUCTION_CLOSE";
    public static final String AUCTION_MARK_PAID = "AUCTION_MARK_PAID";
    public static final String AUCTION_CANCEL_FINISHED = "AUCTION_CANCEL_FINISHED";
    public static final String AUCTION_START_EARLY_CLOSE = "AUCTION_START_EARLY_CLOSE";
    public static final String AUCTION_CANCEL_EARLY_CLOSE = "AUCTION_CANCEL_EARLY_CLOSE";
    public static final String AUCTION_EARLY_CLOSES = "AUCTION_EARLY_CLOSES";
    public static final String AUCTION_REFRESH_STATUSES = "AUCTION_REFRESH_STATUSES";

    public static final String AUTOBID_CREATE = "AUTOBID_CREATE";
    public static final String AUTOBID_UPDATE = "AUTOBID_UPDATE";
    public static final String AUTOBID_DELETE = "AUTOBID_DELETE";
    public static final String AUTOBID_BY_BIDDER = "AUTOBID_BY_BIDDER";
    public static final String AUTOBID_BY_ID = "AUTOBID_BY_ID";

    public static final String WALLET_GET = "WALLET_GET";
    public static final String WALLET_TOP_UP = "WALLET_TOP_UP";
    public static final String WALLET_TOP_UP_HISTORY = "WALLET_TOP_UP_HISTORY";

    public static final String HOMEPAGE_ALL = "HOMEPAGE_ALL";
    public static final String HOMEPAGE_SAVE = "HOMEPAGE_SAVE";
    public static final String HOMEPAGE_DELETE = "HOMEPAGE_DELETE";

    public static final String NOTIFICATION_CREATE = "NOTIFICATION_CREATE";
    public static final String NOTIFICATION_GET = "NOTIFICATION_GET";
}
