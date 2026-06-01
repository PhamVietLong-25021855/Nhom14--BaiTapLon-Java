package userauth.remote;

import userauth.api.AuctionApi;
import userauth.exception.*;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.network.NetworkActions;

import java.util.List;
import java.util.Map;

/** AuctionService chạy ở client: chuyển toàn bộ logic đấu giá sang Server. */
public class RemoteAuctionService implements AuctionApi {
    private final RemoteAuctionClient client;

    public RemoteAuctionService(RemoteAuctionClient client) {
        this.client = client;
    }

    @Override
    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime,
                              String category, String imageSource, byte[] imageData, int sellerId) throws ValidationException {
        String result = result(NetworkActions.AUCTION_CREATE,
                "name", name, "desc", desc, "startPrice", startPrice, "startTime", startTime,
                "endTime", endTime, "category", category, "imageSource", imageSource,
                "imageData", imageData, "sellerId", sellerId);
        if (!"SUCCESS".equals(result)) throw new ValidationException(result);
    }

    @Override
    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice, long startTime,
                              long endTime, String category, String imageSource, byte[] imageData)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        String result = result(NetworkActions.AUCTION_UPDATE,
                "auctionId", auctionId, "sellerId", sellerId, "name", name, "desc", desc,
                "startPrice", startPrice, "startTime", startTime, "endTime", endTime,
                "category", category, "imageSource", imageSource, "imageData", imageData);
        if (!"SUCCESS".equals(result)) throw new ValidationException(result);
    }

    @Override
    public void deleteAuction(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException {
        String result = result(NetworkActions.AUCTION_DELETE, "auctionId", auctionId, "sellerId", sellerId);
        if (!"SUCCESS".equals(result)) throw new UnauthorizedException(result);
    }

    @Override
    public void deleteAuctionAsAdmin(int auctionId) throws ItemNotFoundException {
        String result = result(NetworkActions.AUCTION_ADMIN_DELETE, "auctionId", auctionId);
        if (!"SUCCESS".equals(result)) throw new ItemNotFoundException(result);
    }

    @Override
    public AuctionItem getAuctionById(int auctionId) throws ItemNotFoundException {
        return (AuctionItem) client.call(NetworkActions.AUCTION_GET, "auctionId", auctionId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        return (List<AuctionItem>) client.call(NetworkActions.AUCTION_BY_SELLER, "sellerId", sellerId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AuctionItem> getAllAuctions() {
        return (List<AuctionItem>) client.call(NetworkActions.AUCTION_ALL);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AuctionItem> getAllAuctionSummaries() {
        return (List<AuctionItem>) client.call(NetworkActions.AUCTION_ALL_SUMMARIES);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BidTransaction> getBidsForAuction(int auctionId) {
        return (List<BidTransaction>) client.call(NetworkActions.AUCTION_BIDS, "auctionId", auctionId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BidTransaction> getAllBids() {
        return (List<BidTransaction>) client.call(NetworkActions.AUCTION_ALL_BIDS);
    }

    @Override
    public int countAllBids() {
        Object result = client.call(NetworkActions.AUCTION_BID_COUNT);
        return result instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(result));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Integer> getBidCounts() {
        return (Map<Integer, Integer>) client.call(NetworkActions.AUCTION_BID_COUNTS);
    }

    @Override
    public void placeBid(int auctionId, int bidderId, double amount)
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        try {
            String result = result(NetworkActions.AUCTION_PLACE_BID, "auctionId", auctionId, "bidderId", bidderId, "amount", amount);
            if (!"SUCCESS".equals(result)) throw new InvalidBidException(result);
        } catch (RemoteServerException ex) {
            if ("ItemNotFoundException".equals(ex.getErrorType())) {
                throw new ItemNotFoundException(ex.getMessage());
            }
            if ("AuctionClosedException".equals(ex.getErrorType())) {
                throw new AuctionClosedException(ex.getMessage());
            }
            throw new InvalidBidException(ex.getMessage());
        }
    }

    @Override
    public void closeAuctionManually(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, AuctionClosedException {
        String result = result(NetworkActions.AUCTION_CLOSE, "auctionId", auctionId, "sellerId", sellerId);
        if (!"SUCCESS".equals(result)) throw new AuctionClosedException(result);
    }

    @Override
    public void startAdminEarlyCloseCountdown(int auctionId)
            throws ItemNotFoundException, AuctionClosedException, ValidationException {
        String result = result(NetworkActions.AUCTION_START_EARLY_CLOSE, "auctionId", auctionId);
        if (!"SUCCESS".equals(result)) throw new ValidationException(result);
    }

    @Override
    public void cancelAdminEarlyCloseCountdown(int auctionId) throws ItemNotFoundException, ValidationException {
        String result = result(NetworkActions.AUCTION_CANCEL_EARLY_CLOSE, "auctionId", auctionId);
        if (!"SUCCESS".equals(result)) throw new ValidationException(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Integer> getAdminEarlyCloseCountdowns() {
        return (Map<Integer, Integer>) client.call(NetworkActions.AUCTION_EARLY_CLOSES);
    }

    @Override
    public void markAuctionAsPaid(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        String result = result(NetworkActions.AUCTION_MARK_PAID, "auctionId", auctionId, "sellerId", sellerId);
        if (!"SUCCESS".equals(result)) throw new ValidationException(result);
    }

    @Override
    public void cancelFinishedAuction(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        String result = result(NetworkActions.AUCTION_CANCEL_FINISHED, "auctionId", auctionId, "sellerId", sellerId);
        if (!"SUCCESS".equals(result)) throw new ValidationException(result);
    }

    @Override
    public void refreshAuctionStatuses() {
        client.call(NetworkActions.AUCTION_REFRESH_STATUSES);
    }

    private String result(String action, Object... keyValues) {
        return (String) client.call(action, keyValues);
    }
}
