package userauth.api;

import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;

import java.util.List;
import java.util.Map;

public interface AuctionApi {
    void createAuction(String name, String desc, double startPrice, long startTime, long endTime,
                       String category, String imageSource, byte[] imageData, int sellerId)
            throws ValidationException;

    void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice,
                       long startTime, long endTime, String category, String imageSource, byte[] imageData)
            throws ItemNotFoundException, UnauthorizedException, ValidationException;

    void deleteAuction(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException;

    List<AuctionItem> getAuctionsBySeller(int sellerId);

    List<AuctionItem> getAllAuctions();

    List<BidTransaction> getBidsForAuction(int auctionId);

    List<BidTransaction> getAllBids();

    void placeBid(int auctionId, int bidderId, double amount)
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException;

    void closeAuctionManually(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, AuctionClosedException;

    void startAdminEarlyCloseCountdown(int auctionId)
            throws ItemNotFoundException, AuctionClosedException, ValidationException;

    void cancelAdminEarlyCloseCountdown(int auctionId) throws ItemNotFoundException, ValidationException;

    Map<Integer, Integer> getAdminEarlyCloseCountdowns();

    void markAuctionAsPaid(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException;

    void cancelFinishedAuction(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException;

    void refreshAuctionStatuses();
}
