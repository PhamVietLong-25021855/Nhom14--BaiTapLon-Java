package userauth.service;

import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.exception.*;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class AutobidService {

    private final AutoBidDAO autoBidDAO;
    private final AuctionDAO auctionDAO;

    public AutobidService(AutoBidDAO autoBidDAO, AuctionDAO auctionDAO) {
        this.autoBidDAO = autoBidDAO;
        this.auctionDAO = auctionDAO;
    }

    public void createAutobid(int bidderId, int auctionId, double maxPrice, double increment)
            throws ValidationException {
        AuctionItem auction = requireValidAuctionForAutobid(auctionId, bidderId, maxPrice, increment);
        if (autoBidDAO.findAutoBidByAuctionBidder(auctionId, bidderId) != null) {
            throw new ValidationException("Already existed autobid for this auction");
        }
        AutoBid item = new AutoBid(0, auctionId, bidderId, maxPrice, increment);
        autoBidDAO.saveAutoBid(item);
    }

    public void updateAutobid(int bidderId, int id,double maxPrice, double increment)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        AutoBid item = autoBidDAO.findAutoBidById(id);
        if (item == null) {
            throw new ItemNotFoundException("AutoBid item not found.");
        }
        if (item.getBidderId() != bidderId) {
            throw new UnauthorizedException("Only the creator can edit this item.");
        }
        requireValidAuctionForAutobid(item.getAuctionId(), bidderId, maxPrice, increment);
        item.setMaxPrice(maxPrice);
        item.setIncrement(increment);
        autoBidDAO.updateAutoBid(item);
    }

    public void deleteAutobid(int bidderId, int id) throws ItemNotFoundException, UnauthorizedException {
        AutoBid item = autoBidDAO.findAutoBidById(id);
        if (item == null) {
            throw new ItemNotFoundException("AutoBid item not found.");
        }
        if (item.getBidderId() != bidderId) {
            throw new UnauthorizedException("Only the creator can edit this item.");
        }
        autoBidDAO.deleteAutoBid(id);
    }

    public List<AutoBid> getAutobidByBidder(int bidderId) {
        return autoBidDAO.findAllUserAutoBid(bidderId);
    }
    public AutoBid getAutobid(int id) {
        return autoBidDAO.findAutoBidById(id);
    }

    private AuctionItem requireValidAuctionForAutobid(int auctionId, int bidderId, double maxPrice, double increment)
            throws ValidationException {
        if (maxPrice <= 0) {
            throw new ValidationException("Max price must be greater than 0.");
        }
        if (increment <= 0) {
            throw new ValidationException("Increment must be greater than 0.");
        }

        AuctionItem auction = auctionDAO.findAuctionById(auctionId);
        if (auction == null) {
            throw new ValidationException("Auction not found.");
        }
        if (auction.getSellerId() == bidderId) {
            throw new ValidationException("Sellers cannot create auto-bids for their own auctions.");
        }
        if (auction.getStatus() == AuctionStatus.FINISHED
                || auction.getStatus() == AuctionStatus.PAID
                || auction.getStatus() == AuctionStatus.CANCELED) {
            throw new ValidationException("Auto-bid is only available while the auction is open or running.");
        }
        if (maxPrice <= auction.getCurrentHighestBid()) {
            throw new ValidationException("Max price must be higher than the current highest bid.");
        }
        return auction;
    }
}
