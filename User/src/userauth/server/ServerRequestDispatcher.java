package userauth.server;

import userauth.common.RemoteAction;
import userauth.common.RemoteRequest;
import userauth.common.RemoteResponse;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.HomepageController;
import userauth.controller.WalletController;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.model.Admin;
import userauth.model.AuctionItem;
import userauth.model.AutoBid;
import userauth.model.BidRequest;
import userauth.model.BidResult;
import userauth.model.BidTransaction;
import userauth.model.Bidder;
import userauth.model.HomepageAnnouncement;
import userauth.model.PaymentMethod;
import userauth.model.Role;
import userauth.model.Seller;
import userauth.model.TopUpTransaction;
import userauth.model.User;
import userauth.model.Wallet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ServerRequestDispatcher {
    private final AuthController authController;
    private final AuctionController auctionController;
    private final AutobidController autobidController;
    private final HomepageController homepageController;
    private final WalletController walletController;

    public ServerRequestDispatcher(AuthController authController,
                                   AuctionController auctionController,
                                   AutobidController autobidController,
                                   HomepageController homepageController,
                                   WalletController walletController) {
        this.authController = authController;
        this.auctionController = auctionController;
        this.autobidController = autobidController;
        this.homepageController = homepageController;
        this.walletController = walletController;
    }

    public RemoteResponse handle(RemoteRequest request) {
        try {
            return switch (request.getAction()) {
                case SYSTEM_PING -> RemoteResponse.success("PONG");

                case AUTH_REGISTER_GUI -> RemoteResponse.success(authController.registerGUI(
                        (String) request.argumentAt(0),
                        (String) request.argumentAt(1),
                        (String) request.argumentAt(2),
                        (String) request.argumentAt(3),
                        (Role) request.argumentAt(4)
                ));
                case AUTH_LOGIN -> handleLogin(request);
                case AUTH_GET_ALL_USERS -> RemoteResponse.success(new ArrayList<>(sanitizeUsers(authController.getAllUsersList())));
                case AUTH_CHANGE_PASSWORD -> RemoteResponse.success(authController.changePassword(
                        (String) request.argumentAt(0),
                        (String) request.argumentAt(1),
                        (String) request.argumentAt(2)
                ));
                case AUTH_TOGGLE_USER_STATUS -> RemoteResponse.success(authController.toggleUserStatus(
                        (String) request.argumentAt(0),
                        (Integer) request.argumentAt(1)
                ));
                case AUTH_DELETE_USER -> RemoteResponse.success(authController.deleteUser(
                        (String) request.argumentAt(0),
                        (Integer) request.argumentAt(1)
                ));

                case AUCTION_CREATE -> RemoteResponse.success(auctionController.createAuction(
                        (String) request.argumentAt(0),
                        (String) request.argumentAt(1),
                        (Double) request.argumentAt(2),
                        (Long) request.argumentAt(3),
                        (Long) request.argumentAt(4),
                        (String) request.argumentAt(5),
                        (String) request.argumentAt(6),
                        (Integer) request.argumentAt(7),
                        (Boolean) request.argumentAt(8),
                        (Integer) request.argumentAt(9),
                        (Integer) request.argumentAt(10),
                        (Integer) request.argumentAt(11)
                ));
                case AUCTION_UPDATE -> RemoteResponse.success(auctionController.updateAuction(
                        (Integer) request.argumentAt(0),
                        (Integer) request.argumentAt(1),
                        (String) request.argumentAt(2),
                        (String) request.argumentAt(3),
                        (Double) request.argumentAt(4),
                        (Long) request.argumentAt(5),
                        (Long) request.argumentAt(6),
                        (String) request.argumentAt(7),
                        (String) request.argumentAt(8),
                        (Boolean) request.argumentAt(9),
                        (Integer) request.argumentAt(10),
                        (Integer) request.argumentAt(11),
                        (Integer) request.argumentAt(12)
                ));
                case AUCTION_DELETE -> RemoteResponse.success(auctionController.deleteAuction(
                        (Integer) request.argumentAt(0),
                        (Integer) request.argumentAt(1)
                ));
                case AUCTION_GET_BY_SELLER -> RemoteResponse.success(new ArrayList<>(auctionController.getAuctionsBySeller(
                        (Integer) request.argumentAt(0)
                )));
                case AUCTION_GET_ALL -> RemoteResponse.success(new ArrayList<>(auctionController.getAllAuctions()));
                case AUCTION_GET_BIDS_FOR_AUCTION -> RemoteResponse.success(new ArrayList<>(auctionController.getBidsForAuction(
                        (Integer) request.argumentAt(0)
                )));
                case AUCTION_GET_ALL_BIDS -> RemoteResponse.success(new ArrayList<>(auctionController.getAllBids()));
                case AUCTION_PLACE_BID -> RemoteResponse.success(auctionController.placeBid(
                        (Integer) request.argumentAt(0),
                        (Integer) request.argumentAt(1),
                        (Double) request.argumentAt(2)
                ));
                case AUCTION_PLACE_BID_DETAILED -> handleDetailedBid(request);
                case AUCTION_CLOSE -> RemoteResponse.success(auctionController.closeAuction(
                        (Integer) request.argumentAt(0),
                        (Integer) request.argumentAt(1)
                ));
                case AUCTION_EXTEND_TIME -> RemoteResponse.success(auctionController.extendAuctionTime(
                        (Integer) request.argumentAt(0),
                        (Integer) request.argumentAt(1),
                        (Integer) request.argumentAt(2)
                ));
                case AUCTION_START_ADMIN_EARLY_CLOSE -> RemoteResponse.success(auctionController.startAdminEarlyCloseCountdown(
                        (User) request.argumentAt(0),
                        (Integer) request.argumentAt(1)
                ));
                case AUCTION_CANCEL_ADMIN_EARLY_CLOSE -> RemoteResponse.success(auctionController.cancelAdminEarlyCloseCountdown(
                        (User) request.argumentAt(0),
                        (Integer) request.argumentAt(1)
                ));
                case AUCTION_GET_ADMIN_EARLY_CLOSE_COUNTDOWNS -> RemoteResponse.success(new HashMap<>(auctionController.getAdminEarlyCloseCountdowns()));

                case AUTOBID_CREATE -> RemoteResponse.success(autobidController.createAutobid(
                        (Integer) request.argumentAt(0),
                        (Integer) request.argumentAt(1),
                        (Double) request.argumentAt(2),
                        (Double) request.argumentAt(3)
                ));
                case AUTOBID_UPDATE -> RemoteResponse.success(autobidController.updateAutobid(
                        (Integer) request.argumentAt(0),
                        (Integer) request.argumentAt(1),
                        (Double) request.argumentAt(2),
                        (Double) request.argumentAt(3)
                ));
                case AUTOBID_DELETE -> RemoteResponse.success(autobidController.deleteAutoBid(
                        (Integer) request.argumentAt(0),
                        (Integer) request.argumentAt(1)
                ));
                case AUTOBID_GET_BY_BIDDER -> RemoteResponse.success(new ArrayList<>(autobidController.getAutobidByBidder(
                        (Integer) request.argumentAt(0)
                )));
                case AUTOBID_GET_BY_ID -> RemoteResponse.success(autobidController.getAutobidById(
                        (Integer) request.argumentAt(0)
                ));

                case HOMEPAGE_GET_ALL -> RemoteResponse.success(new ArrayList<>(homepageController.getAllAnnouncements()));
                case HOMEPAGE_SAVE -> RemoteResponse.success(homepageController.saveAnnouncement(
                        (User) request.argumentAt(0),
                        (Integer) request.argumentAt(1),
                        (String) request.argumentAt(2),
                        (String) request.argumentAt(3),
                        (String) request.argumentAt(4),
                        (String) request.argumentAt(5),
                        (Integer) request.argumentAt(6)
                ));
                case HOMEPAGE_DELETE -> RemoteResponse.success(homepageController.deleteAnnouncement(
                        (User) request.argumentAt(0),
                        (Integer) request.argumentAt(1)
                ));

                case WALLET_INITIALIZE -> RemoteResponse.success(walletController.initializeWallet(
                        (Integer) request.argumentAt(0)
                ));
                case WALLET_CREATE_TOPUP -> RemoteResponse.success(walletController.createTopUpRequest(
                        (Integer) request.argumentAt(0),
                        (Double) request.argumentAt(1),
                        (PaymentMethod) request.argumentAt(2)
                ));
                case WALLET_CONFIRM_TOPUP -> RemoteResponse.success(walletController.confirmTopUp(
                        (Integer) request.argumentAt(0),
                        (String) request.argumentAt(1)
                ));
                case WALLET_CANCEL_TOPUP -> RemoteResponse.success(walletController.cancelTopUp(
                        (Integer) request.argumentAt(0)
                ));
                case WALLET_GET_WALLET -> RemoteResponse.success(walletController.getWallet(
                        (Integer) request.argumentAt(0)
                ));
                case WALLET_DEDUCT -> RemoteResponse.success(walletController.deductFromWallet(
                        (Integer) request.argumentAt(0),
                        (Double) request.argumentAt(1)
                ));
                case WALLET_ADD -> RemoteResponse.success(walletController.addToWallet(
                        (Integer) request.argumentAt(0),
                        (Double) request.argumentAt(1)
                ));
                case WALLET_GET_TOPUP_HISTORY -> RemoteResponse.success(new ArrayList<>(walletController.getTopUpHistory(
                        (Integer) request.argumentAt(0)
                )));
                case WALLET_GET_ALL_PENDING -> RemoteResponse.success(new ArrayList<>(walletController.getAllPendingTrasactions()));
                case WALLET_GET_BALANCE -> RemoteResponse.success(walletController.getWalletBalance(
                        (Integer) request.argumentAt(0)
                ));
            };
        } catch (UnauthorizedException | ItemNotFoundException | AuctionClosedException | InvalidBidException ex) {
            return RemoteResponse.failure(ex.getMessage(), ex.getClass().getSimpleName());
        } catch (Exception ex) {
            return RemoteResponse.failure(ex.getMessage(), ex.getClass().getSimpleName());
        }
    }

    private RemoteResponse handleLogin(RemoteRequest request) throws UnauthorizedException {
        User user = authController.login(
                (String) request.argumentAt(0),
                (String) request.argumentAt(1)
        );
        return RemoteResponse.success(sanitizeUser(user));
    }

    private RemoteResponse handleDetailedBid(RemoteRequest request)
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        BidResult result = auctionController.placeBidDetailed((BidRequest) request.argumentAt(0));
        return RemoteResponse.success(result);
    }

    private List<User> sanitizeUsers(List<User> users) {
        List<User> sanitized = new ArrayList<>();
        for (User user : users) {
            sanitized.add(sanitizeUser(user));
        }
        return sanitized;
    }

    private User sanitizeUser(User user) {
        if (user == null) {
            return null;
        }

        long createdAt = user.getCreatedAt();
        long updatedAt = user.getUpdatedAt();
        String password = "";
        return switch (user.getRole()) {
            case ADMIN -> new Admin(
                    user.getId(),
                    user.getUsername(),
                    password,
                    user.getFullName(),
                    user.getEmail(),
                    user.getStatus(),
                    createdAt,
                    updatedAt,
                    user instanceof Admin admin ? admin.getDepartment() : "SYSTEM"
            );
            case SELLER -> new Seller(
                    user.getId(),
                    user.getUsername(),
                    password,
                    user.getFullName(),
                    user.getEmail(),
                    user.getStatus(),
                    createdAt,
                    updatedAt
            );
            case BIDDER -> new Bidder(
                    user.getId(),
                    user.getUsername(),
                    password,
                    user.getFullName(),
                    user.getEmail(),
                    user.getStatus(),
                    createdAt,
                    updatedAt
            );
        };
    }
}
