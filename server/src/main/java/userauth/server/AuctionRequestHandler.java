package userauth.server;

import userauth.model.Role;
import userauth.model.PaymentMethod;
import userauth.network.AuctionRequest;
import userauth.network.AuctionResponse;
import userauth.network.NetworkActions;

/** Xử lý một request socket và gọi đúng Controller/Service trên server. */
public final class AuctionRequestHandler {
    private final ServerContext context;

    public AuctionRequestHandler(ServerContext context) {
        this.context = context;
    }

    public AuctionResponse handle(AuctionRequest request) {
        try {
            return AuctionResponse.ok(dispatch(request));
        } catch (Throwable ex) {
            return AuctionResponse.fail(ex);
        }
    }

    private Object dispatch(AuctionRequest request) throws Exception {
        String action = request.getAction();
        if (NetworkActions.PING.equals(action)) {
            return "PONG";
        }
        System.out.println(action);
        return switch (action) {
            case NetworkActions.AUTH_REGISTER -> context.getAuthController().registerGUI(
                    str(request, "username"), str(request, "password"), str(request, "fullName"),
                    str(request, "email"), (Role) request.get("role"));
            case NetworkActions.AUTH_LOGIN -> context.getAuthController().login(str(request, "username"), str(request, "password"));
            case NetworkActions.AUTH_ALL_USERS -> context.getAuthController().getAllUsersList();
            case NetworkActions.AUTH_CHANGE_PASSWORD -> context.getAuthController().changePassword(
                    str(request, "username"), str(request, "oldPassword"), str(request, "newPassword"));
            case NetworkActions.AUTH_UPDATE_PROFILE -> context.getAuthController().updateProfile(
                    str(request, "username"), str(request, "fullName"), str(request, "email"));
            case NetworkActions.AUTH_TOGGLE_STATUS -> context.getAuthController().toggleUserStatus(
                    str(request, "adminUsername"), integer(request, "targetUserId"));
            case NetworkActions.AUTH_DELETE_USER -> context.getAuthController().deleteUserAccount(
                    str(request, "adminUsername"), integer(request, "targetUserId"));

            case NetworkActions.AUCTION_CREATE -> context.getAuctionController().createAuction(
                    str(request, "name"), str(request, "desc"), dbl(request, "startPrice"), lng(request, "startTime"),
                    lng(request, "endTime"), str(request, "category"), str(request, "imageSource"),
                    (byte[]) request.get("imageData"), integer(request, "sellerId"));
            case NetworkActions.AUCTION_UPDATE -> context.getAuctionController().updateAuction(
                    integer(request, "auctionId"), integer(request, "sellerId"), str(request, "name"), str(request, "desc"),
                    dbl(request, "startPrice"), lng(request, "startTime"), lng(request, "endTime"), str(request, "category"),
                    str(request, "imageSource"), (byte[]) request.get("imageData"));
            case NetworkActions.AUCTION_DELETE -> context.getAuctionController().deleteAuction(integer(request, "auctionId"), integer(request, "sellerId"));
            case NetworkActions.AUCTION_BY_SELLER -> context.getAuctionController().getAuctionsBySeller(integer(request, "sellerId"));
            case NetworkActions.AUCTION_ALL -> context.getAuctionController().getAllAuctions();
            case NetworkActions.AUCTION_BIDS -> context.getAuctionController().getBidsForAuction(integer(request, "auctionId"));
            case NetworkActions.AUCTION_ALL_BIDS -> context.getAuctionController().getAllBids();
            case NetworkActions.AUCTION_PLACE_BID -> context.getAuctionController().placeBid(
                    integer(request, "auctionId"), integer(request, "bidderId"), dbl(request, "amount"));
            case NetworkActions.AUCTION_CLOSE -> context.getAuctionController().closeAuction(integer(request, "auctionId"), integer(request, "sellerId"));
            case NetworkActions.AUCTION_MARK_PAID -> context.getAuctionController().markAuctionAsPaid(integer(request, "auctionId"), integer(request, "sellerId"));
            case NetworkActions.AUCTION_CANCEL_FINISHED -> context.getAuctionController().cancelFinishedAuction(integer(request, "auctionId"), integer(request, "sellerId"));
            case NetworkActions.AUCTION_START_EARLY_CLOSE -> {
                context.getAuctionService().startAdminEarlyCloseCountdown(integer(request, "auctionId"));
                yield "SUCCESS";
            }
            case NetworkActions.AUCTION_CANCEL_EARLY_CLOSE -> {
                context.getAuctionService().cancelAdminEarlyCloseCountdown(integer(request, "auctionId"));
                yield "SUCCESS";
            }
            case NetworkActions.AUCTION_EARLY_CLOSES -> context.getAuctionController().getAdminEarlyCloseCountdowns();
            case NetworkActions.AUCTION_REFRESH_STATUSES -> {
                context.getAuctionController().getAllAuctions();
                yield "SUCCESS";
            }

            case NetworkActions.AUTOBID_CREATE -> context.getAutobidController().createAutobid(
                    integer(request, "bidderId"), integer(request, "auctionId"), dbl(request, "maxPrice"), dbl(request, "increment"));
            case NetworkActions.AUTOBID_UPDATE -> context.getAutobidController().updateAutobid(
                    integer(request, "bidderId"), integer(request, "id"), dbl(request, "maxPrice"), dbl(request, "increment"));
            case NetworkActions.AUTOBID_DELETE -> context.getAutobidController().deleteAutoBid(integer(request, "bidderId"), integer(request, "id"));
            case NetworkActions.AUTOBID_BY_BIDDER -> context.getAutobidController().getAutobidByBidder(integer(request, "bidderId"));
            case NetworkActions.AUTOBID_BY_ID -> context.getAutobidController().getAutobidById(integer(request, "id"));

            case NetworkActions.WALLET_GET -> context.getWalletController().getWallet(integer(request, "userId"));
            case NetworkActions.WALLET_TOP_UP -> context.getWalletController().createTopUpRequest(
                    integer(request, "userId"), lng(request, "amount"), (PaymentMethod) request.get("method"));
            case NetworkActions.WALLET_TOP_UP_HISTORY -> context.getWalletController().getTopUpHistory(integer(request, "userId"));

            case NetworkActions.HOMEPAGE_ALL -> context.getHomepageController().getAllAnnouncements();
            case NetworkActions.HOMEPAGE_SAVE -> {
                context.getHomepageContentService().saveAnnouncement(
                        (Integer) request.get("announcementId"), str(request, "title"), str(request, "summary"),
                        str(request, "details"), str(request, "scheduleText"),
                        (Integer) request.get("linkedAuctionId"), integer(request, "authorId"));
                yield "SUCCESS";
            }
            case NetworkActions.HOMEPAGE_DELETE -> {
                context.getHomepageContentService().deleteAnnouncement(integer(request, "announcementId"));
                yield "SUCCESS";
            }

            case NetworkActions.NOTIFICATION_CREATE -> context.getNotificationController().createNotification(integer(request, "user_id"),str(request, "title"), str(request, "content"));
            case NetworkActions.NOTIFICATION_GET -> context.getNotificationController().findUserNotification(integer(request,"user_id"));

            default -> throw new IllegalArgumentException("Unsupported network action: " + action);
        };
    }

    private String str(AuctionRequest request, String key) {
        Object value = request.get(key);
        return value == null ? null : value.toString();
    }

    private int integer(AuctionRequest request, String key) {
        Object value = request.get(key);
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private long lng(AuctionRequest request, String key) {
        Object value = request.get(key);
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private double dbl(AuctionRequest request, String key) {
        Object value = request.get(key);
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
    }
}
