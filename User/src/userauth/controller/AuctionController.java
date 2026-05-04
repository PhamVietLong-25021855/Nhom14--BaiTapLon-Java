package userauth.controller;

import userauth.client.network.RemoteApiClient;
import userauth.common.RemoteAction;
import userauth.common.RemoteResponse;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.BidRequest;
import userauth.model.BidResult;
import userauth.model.BidTransaction;
import userauth.model.Role;
import userauth.model.User;
import userauth.service.AuctionService;

import java.util.List;
import java.util.Map;

public class AuctionController extends RemoteControllerSupport {
    private final AuctionService auctionService;
    private final RemoteApiClient remoteApiClient;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
        this.remoteApiClient = null;
    }

    public AuctionController(RemoteApiClient remoteApiClient) {
        this.auctionService = null;
        this.remoteApiClient = remoteApiClient;
    }

    public String createAuction(String name, String desc, double startPrice, long startTime, long endTime,
                                String category, String imageSource, int sellerId) {
        return createAuction(
                name,
                desc,
                startPrice,
                startTime,
                endTime,
                category,
                imageSource,
                sellerId,
                true,
                AuctionItem.DEFAULT_EXTENSION_THRESHOLD_SECONDS,
                AuctionItem.DEFAULT_EXTENSION_DURATION_SECONDS,
                AuctionItem.DEFAULT_MAX_EXTENSION_COUNT
        );
    }

    public String createAuction(String name, String desc, double startPrice, long startTime, long endTime,
                                String category, String imageSource, int sellerId, boolean antiSnipingEnabled,
                                int extensionThresholdSeconds, int extensionDurationSeconds, int maxExtensionCount) {
        if (remoteApiClient != null) {
            return requestString(
                    RemoteAction.AUCTION_CREATE,
                    name,
                    desc,
                    startPrice,
                    startTime,
                    endTime,
                    category,
                    imageSource,
                    sellerId,
                    antiSnipingEnabled,
                    extensionThresholdSeconds,
                    extensionDurationSeconds,
                    maxExtensionCount
            );
        }

        try {
            auctionService.createAuction(
                    name,
                    desc,
                    startPrice,
                    startTime,
                    endTime,
                    category,
                    imageSource,
                    sellerId,
                    antiSnipingEnabled,
                    extensionThresholdSeconds,
                    extensionDurationSeconds,
                    maxExtensionCount
            );
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public String updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice,
                                long startTime, long endTime, String category, String imageSource) {
        return updateAuction(
                auctionId,
                sellerId,
                name,
                desc,
                startPrice,
                startTime,
                endTime,
                category,
                imageSource,
                true,
                AuctionItem.DEFAULT_EXTENSION_THRESHOLD_SECONDS,
                AuctionItem.DEFAULT_EXTENSION_DURATION_SECONDS,
                AuctionItem.DEFAULT_MAX_EXTENSION_COUNT
        );
    }

    public String updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice,
                                long startTime, long endTime, String category, String imageSource,
                                boolean antiSnipingEnabled, int extensionThresholdSeconds,
                                int extensionDurationSeconds, int maxExtensionCount) {
        if (remoteApiClient != null) {
            return requestString(
                    RemoteAction.AUCTION_UPDATE,
                    auctionId,
                    sellerId,
                    name,
                    desc,
                    startPrice,
                    startTime,
                    endTime,
                    category,
                    imageSource,
                    antiSnipingEnabled,
                    extensionThresholdSeconds,
                    extensionDurationSeconds,
                    maxExtensionCount
            );
        }

        try {
            auctionService.updateAuction(
                    auctionId,
                    sellerId,
                    name,
                    desc,
                    startPrice,
                    startTime,
                    endTime,
                    category,
                    imageSource,
                    antiSnipingEnabled,
                    extensionThresholdSeconds,
                    extensionDurationSeconds,
                    maxExtensionCount
            );
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String deleteAuction(int auctionId, int sellerId) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUCTION_DELETE, auctionId, sellerId);
        }

        try {
            auctionService.deleteAuction(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        if (remoteApiClient != null) {
            return requestList(remoteApiClient, RemoteAction.AUCTION_GET_BY_SELLER, sellerId);
        }
        return auctionService.getAuctionsBySeller(sellerId);
    }

    public List<AuctionItem> getAllAuctions() {
        if (remoteApiClient != null) {
            return requestList(remoteApiClient, RemoteAction.AUCTION_GET_ALL);
        }
        return auctionService.getAllAuctions();
    }

    public List<BidTransaction> getBidsForAuction(int auctionId) {
        if (remoteApiClient != null) {
            return requestList(remoteApiClient, RemoteAction.AUCTION_GET_BIDS_FOR_AUCTION, auctionId);
        }
        return auctionService.getBidsForAuction(auctionId);
    }

    public List<BidTransaction> getAllBids() {
        if (remoteApiClient != null) {
            return requestList(remoteApiClient, RemoteAction.AUCTION_GET_ALL_BIDS);
        }
        return auctionService.getAllBids();
    }

    public String placeBid(int auctionId, int bidderId, double amount) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUCTION_PLACE_BID, auctionId, bidderId, amount);
        }

        try {
            auctionService.placeBid(auctionId, bidderId, amount);
            return "SUCCESS";
        } catch (ItemNotFoundException | AuctionClosedException | InvalidBidException e) {
            return e.getMessage();
        }
    }

    public BidResult placeBidDetailed(BidRequest request)
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        if (remoteApiClient != null) {
            RemoteResponse response = remoteApiClient.send(RemoteAction.AUCTION_PLACE_BID_DETAILED, request);
            if (!response.isSuccess()) {
                throwMappedBidException(response);
            }
            return response.payloadAs(BidResult.class);
        }

        return auctionService.placeBid(request);
    }

    public String closeAuction(int auctionId, int sellerId) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUCTION_CLOSE, auctionId, sellerId);
        }

        try {
            auctionService.closeAuctionManually(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | AuctionClosedException e) {
            return e.getMessage();
        }
    }

    public String extendAuctionTime(int auctionId, int sellerId, int additionalMinutes) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUCTION_EXTEND_TIME, auctionId, sellerId, additionalMinutes);
        }

        try {
            auctionService.extendAuctionTime(auctionId, sellerId, additionalMinutes);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String startAdminEarlyCloseCountdown(User currentUser, int auctionId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "Only admins can issue early-close commands.";
        }
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUCTION_START_ADMIN_EARLY_CLOSE, currentUser, auctionId);
        }

        try {
            auctionService.startAdminEarlyCloseCountdown(auctionId);
            return "SUCCESS";
        } catch (ItemNotFoundException | AuctionClosedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String cancelAdminEarlyCloseCountdown(User currentUser, int auctionId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "Only admins can cancel early-close commands.";
        }
        if (remoteApiClient != null) {
            return requestString(RemoteAction.AUCTION_CANCEL_ADMIN_EARLY_CLOSE, currentUser, auctionId);
        }

        try {
            auctionService.cancelAdminEarlyCloseCountdown(auctionId);
            return "SUCCESS";
        } catch (ItemNotFoundException | ValidationException e) {
            return e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, Integer> getAdminEarlyCloseCountdowns() {
        if (remoteApiClient != null) {
            return requestMap(remoteApiClient, RemoteAction.AUCTION_GET_ADMIN_EARLY_CLOSE_COUNTDOWNS);
        }

        return auctionService.getAdminEarlyCloseCountdowns();
    }

    private String requestString(RemoteAction action, Object... arguments) {
        return requestString(remoteApiClient, action, arguments);
    }

    private void throwMappedBidException(RemoteResponse response)
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        String message = response.getMessage();
        String errorType = response.getErrorType();
        if (ItemNotFoundException.class.getSimpleName().equals(errorType)) {
            throw new ItemNotFoundException(message);
        }
        if (AuctionClosedException.class.getSimpleName().equals(errorType)) {
            throw new AuctionClosedException(message);
        }
        throw new InvalidBidException(message);
    }
}
