package userauth.server;

import userauth.model.Role;
import userauth.model.PaymentMethod;
import userauth.model.AutoBid;
import userauth.exception.UnauthorizedException;
import userauth.network.AuctionRequest;
import userauth.network.AuctionResponse;
import userauth.network.NetworkActions;

/** Xử lý một request socket và gọi đúng Controller/Service trên server. */
public final class AuctionRequestHandler {
    private final ServerContext context;
    private final AuctionSessionManager sessions;

    public AuctionRequestHandler(ServerContext context) {
        this(context, new AuctionSessionManager());
    }

    AuctionRequestHandler(ServerContext context, AuctionSessionManager sessions) {
        this.context = context;
        this.sessions = sessions;
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
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Network action is required.");
        }
        if (NetworkActions.PING.equals(action)) {
            return "PONG";
        }
        System.out.println(action);
        return switch (action) {
            case NetworkActions.AUTH_REGISTER -> context.getAuthController().registerGUI(
                    str(request, "username"), str(request, "password"), str(request, "fullName"),
                    str(request, "email"), (Role) request.get("role"));
            case NetworkActions.AUTH_LOGIN -> sessions.create(context.getAuthController().login(
                    str(request, "username"), str(request, "password")));
            case NetworkActions.AUTH_LOGOUT -> {
                requireAuthenticated(request);
                sessions.invalidate(request.getSessionToken());
                yield "SUCCESS";
            }
            case NetworkActions.AUTH_GET_USER -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                int userId = integer(request, "userId");
                requireSelfOrRole(principal, userId, Role.ADMIN);
                yield context.getAuthController().getUserById(userId);
            }
            case NetworkActions.AUTH_ALL_USERS -> {
                requireRole(request, Role.ADMIN);
                yield context.getAuthController().getAllUsersList();
            }
            case NetworkActions.AUTH_CHANGE_PASSWORD -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                yield context.getAuthController().changePassword(
                        principal.username(), str(request, "oldPassword"), str(request, "newPassword"));
            }
            case NetworkActions.AUTH_UPDATE_PROFILE -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                yield context.getAuthController().updateProfile(
                        principal.username(), str(request, "fullName"), str(request, "email"));
            }
            case NetworkActions.AUTH_TOGGLE_STATUS -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.ADMIN);
                int targetUserId = integer(request, "targetUserId");
                String result = context.getAuthController().toggleUserStatus(principal.username(), targetUserId);
                sessions.invalidateUser(targetUserId);
                yield result;
            }
            case NetworkActions.AUTH_DELETE_USER -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.ADMIN);
                int targetUserId = integer(request, "targetUserId");
                String result = context.getAuthController().deleteUserAccount(principal.username(), targetUserId);
                sessions.invalidateUser(targetUserId);
                yield result;
            }

            case NetworkActions.AUCTION_CREATE -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.SELLER);
                yield context.getAuctionController().createAuction(
                    str(request, "name"), str(request, "desc"), dbl(request, "startPrice"), lng(request, "startTime"),
                    lng(request, "endTime"), str(request, "category"), str(request, "imageSource"),
                    (byte[]) request.get("imageData"), dbl(request, "bidStep"), principal.userId());
            }
            case NetworkActions.AUCTION_UPDATE -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.SELLER);
                yield context.getAuctionController().updateAuction(
                    integer(request, "auctionId"), principal.userId(), str(request, "name"), str(request, "desc"),
                    dbl(request, "startPrice"), lng(request, "startTime"), lng(request, "endTime"), str(request, "category"),
                    str(request, "imageSource"), (byte[]) request.get("imageData"), dbl(request, "bidStep"));
            }
            case NetworkActions.AUCTION_DELETE -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.SELLER);
                yield context.getAuctionController().deleteAuction(integer(request, "auctionId"), principal.userId());
            }
            case NetworkActions.AUCTION_ADMIN_DELETE -> {
                requireRole(request, Role.ADMIN);
                context.getAuctionService().deleteAuctionAsAdmin(integer(request, "auctionId"));
                yield "SUCCESS";
            }
            case NetworkActions.AUCTION_GET -> context.getAuctionController().getAuctionById(integer(request, "auctionId"));
            case NetworkActions.AUCTION_BY_SELLER -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.SELLER, Role.ADMIN);
                int sellerId = integer(request, "sellerId");
                requireSelfOrRole(principal, sellerId, Role.ADMIN);
                yield context.getAuctionController().getAuctionsBySeller(sellerId);
            }
            case NetworkActions.AUCTION_ALL -> context.getAuctionController().getAllAuctions();
            case NetworkActions.AUCTION_ALL_SUMMARIES -> context.getAuctionController().getAllAuctionSummaries();
            case NetworkActions.AUCTION_BIDS -> {
                requireAuthenticated(request);
                yield context.getAuctionController().getBidsForAuction(integer(request, "auctionId"));
            }
            case NetworkActions.AUCTION_ALL_BIDS -> {
                requireAuthenticated(request);
                yield context.getAuctionController().getAllBids();
            }
            case NetworkActions.AUCTION_BID_COUNT -> {
                requireRole(request, Role.ADMIN);
                yield context.getAuctionController().countAllBids();
            }
            case NetworkActions.AUCTION_BID_COUNTS -> context.getAuctionController().getBidCounts();
            case NetworkActions.AUCTION_PLACE_BID -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.BIDDER);
                yield context.getAuctionController().placeBid(
                        integer(request, "auctionId"), principal.userId(), dbl(request, "amount"));
            }
            case NetworkActions.AUCTION_CLOSE -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.SELLER);
                yield context.getAuctionController().closeAuction(integer(request, "auctionId"), principal.userId());
            }
            case NetworkActions.AUCTION_MARK_PAID -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.SELLER);
                yield context.getAuctionController().markAuctionAsPaid(integer(request, "auctionId"), principal.userId());
            }
            case NetworkActions.AUCTION_CANCEL_FINISHED -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.SELLER);
                yield context.getAuctionController().cancelFinishedAuction(integer(request, "auctionId"), principal.userId());
            }
            case NetworkActions.AUCTION_START_EARLY_CLOSE -> {
                requireRole(request, Role.ADMIN);
                context.getAuctionService().startAdminEarlyCloseCountdown(integer(request, "auctionId"));
                yield "SUCCESS";
            }
            case NetworkActions.AUCTION_CANCEL_EARLY_CLOSE -> {
                requireRole(request, Role.ADMIN);
                context.getAuctionService().cancelAdminEarlyCloseCountdown(integer(request, "auctionId"));
                yield "SUCCESS";
            }
            case NetworkActions.AUCTION_EARLY_CLOSES -> {
                requireRole(request, Role.ADMIN);
                yield context.getAuctionController().getAdminEarlyCloseCountdowns();
            }
            case NetworkActions.AUCTION_REFRESH_STATUSES -> {
                requireRole(request, Role.ADMIN);
                context.getAuctionController().refreshStatuses();
                yield "SUCCESS";
            }

            case NetworkActions.AUTOBID_CREATE -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.BIDDER);
                yield context.getAutobidController().createAutobid(
                        principal.userId(), integer(request, "auctionId"), dbl(request, "maxPrice"), dbl(request, "increment"));
            }
            case NetworkActions.AUTOBID_UPDATE -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.BIDDER);
                yield context.getAutobidController().updateAutobid(
                        principal.userId(), integer(request, "id"), dbl(request, "maxPrice"), dbl(request, "increment"));
            }
            case NetworkActions.AUTOBID_DELETE -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.BIDDER);
                yield context.getAutobidController().deleteAutoBid(principal.userId(), integer(request, "id"));
            }
            case NetworkActions.AUTOBID_BY_BIDDER -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.BIDDER);
                yield context.getAutobidController().getAutobidByBidder(principal.userId());
            }
            case NetworkActions.AUTOBID_BY_ID -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.BIDDER);
                AutoBid autoBid = context.getAutobidController().getAutobidById(integer(request, "id"));
                if (autoBid != null && autoBid.getBidderId() != principal.userId()) {
                    throw new UnauthorizedException("You do not have permission to view this auto-bid rule.");
                }
                yield autoBid;
            }

            case NetworkActions.WALLET_GET -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                requireSelf(principal, integer(request, "userId"));
                yield context.getWalletController().getWallet(principal.userId());
            }
            case NetworkActions.WALLET_TOP_UP -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                requireSelf(principal, integer(request, "userId"));
                yield context.getWalletController().createTopUpRequest(
                        principal.userId(), lng(request, "amount"), (PaymentMethod) request.get("method"));
            }
            case NetworkActions.WALLET_TOP_UP_HISTORY -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                requireSelf(principal, integer(request, "userId"));
                yield context.getWalletController().getTopUpHistory(principal.userId());
            }

            case NetworkActions.HOMEPAGE_ALL -> context.getHomepageController().getAllAnnouncements();
            case NetworkActions.HOMEPAGE_SAVE -> {
                AuctionSessionManager.Session principal = requireRole(request, Role.ADMIN);
                context.getHomepageContentService().saveAnnouncement(
                        (Integer) request.get("announcementId"), str(request, "title"), str(request, "summary"),
                        str(request, "details"), str(request, "scheduleText"),
                        (Integer) request.get("linkedAuctionId"), principal.userId());
                yield "SUCCESS";
            }
            case NetworkActions.HOMEPAGE_DELETE -> {
                requireRole(request, Role.ADMIN);
                context.getHomepageContentService().deleteAnnouncement(integer(request, "announcementId"));
                yield "SUCCESS";
            }

            case NetworkActions.NOTIFICATION_CREATE -> {
                requireRole(request, Role.ADMIN);
                yield context.getNotificationController().createNotification(
                        integer(request, "user_id"), str(request, "title"), str(request, "content"));
            }
            case NetworkActions.NOTIFICATION_GET -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                requireSelf(principal, integer(request, "user_id"));
                yield context.getNotificationController().findUserNotification(principal.userId());
            }
            case NetworkActions.NOTIFICATION_DELETE -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                requireSelf(principal, integer(request, "user_id"));
                yield context.getNotificationController().deleteNotification(
                        principal.userId(), integer(request, "notification_id"));
            }
            case NetworkActions.NOTIFICATION_DELETE_ALL -> {
                AuctionSessionManager.Session principal = requireAuthenticated(request);
                requireSelf(principal, integer(request, "user_id"));
                yield context.getNotificationController().deleteUserNotifications(principal.userId());
            }

            default -> throw new IllegalArgumentException("Unsupported network action: " + action);
        };
    }

    private AuctionSessionManager.Session requireAuthenticated(AuctionRequest request) throws UnauthorizedException {
        return sessions.require(request.getSessionToken());
    }

    private AuctionSessionManager.Session requireRole(AuctionRequest request, Role... roles) throws UnauthorizedException {
        AuctionSessionManager.Session principal = requireAuthenticated(request);
        for (Role role : roles) {
            if (principal.role() == role) {
                return principal;
            }
        }
        throw new UnauthorizedException("You do not have permission to perform this action.");
    }

    private void requireSelf(AuctionSessionManager.Session principal, int requestedUserId) throws UnauthorizedException {
        if (principal.userId() != requestedUserId) {
            throw new UnauthorizedException("You do not have permission to access another account.");
        }
    }

    private void requireSelfOrRole(AuctionSessionManager.Session principal, int requestedUserId, Role role)
            throws UnauthorizedException {
        if (principal.userId() != requestedUserId && principal.role() != role) {
            throw new UnauthorizedException("You do not have permission to access another account.");
        }
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
