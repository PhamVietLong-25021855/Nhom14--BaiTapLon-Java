package userauth.service;

import userauth.api.AutobidApi;
import userauth.dao.AutoBidDAO;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;

import java.util.List;

public class AutobidService implements AutobidApi {

    private final AutoBidDAO autoBidDAO;
    private final AuctionService auctionService;

    public AutobidService(AutoBidDAO autoBidDAO, AuctionService auctionService) {
        this.autoBidDAO = autoBidDAO;
        this.auctionService = auctionService;
    }

    public void createAutobid(int bidderId, int auctionId, double maxPrice, double increment)
            throws ValidationException {
        if (maxPrice <= 0) {
            throw new ValidationException("Max price must be greater than 0.");
        }
        if (increment <= 0) {
            throw new ValidationException("Increment must be greater than 0");
        }
        if (increment > maxPrice) {
            throw new ValidationException("Increment cannot be greater than max price.");
        }
        AutoBid existing = autoBidDAO.findAutoBidByAuctionBidder(auctionId, bidderId);
        if (existing != null) {
            existing.setMaxPrice(maxPrice);
            existing.setIncrement(increment);
            existing.setUpdatedAt(System.currentTimeMillis());
            autoBidDAO.updateAutoBid(existing);
            if (auctionService != null) {
                auctionService.triggerAutoBids(auctionId);
            }
            return;
        }
        long now = System.currentTimeMillis();
        AutoBid item = new AutoBid(0, auctionId, bidderId, maxPrice, increment, now, now);
        autoBidDAO.saveAutoBid(item);
        if (auctionService != null) {
            auctionService.triggerAutoBids(auctionId);
        }
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
        if (maxPrice <= 0) {
            throw new ValidationException("Max price must be greater than 0.");
        }
        if (increment <= 0) {
            throw new ValidationException("Increment must be greater than 0");
        }
        if (increment > maxPrice) {
            throw new ValidationException("Increment cannot be greater than max price.");
        }
        item.setMaxPrice(maxPrice);
        item.setIncrement(increment);
        item.setUpdatedAt(System.currentTimeMillis());
        autoBidDAO.updateAutoBid(item);
        if (auctionService != null) {
            auctionService.triggerAutoBids(item.getAuctionId());
        }
    }

    public void deleteAutobid(int bidderId, int id) throws ItemNotFoundException, UnauthorizedException {
        AutoBid item = autoBidDAO.findAutoBidById(id);
        if (item == null) {
            throw new ItemNotFoundException("AutoBid item not found.");
        }
        if (item.getBidderId() != bidderId) {
            throw new UnauthorizedException("Only the creator can edit this item.");
        }
        // Attempt to delete by id first. If for some reason the delete did not remove the record
        // (e.g., id mismatch in some clients), fall back to deleting by auction/bidder pair.
        autoBidDAO.deleteAutoBid(id);
        AutoBid still = autoBidDAO.findAutoBidById(id);
        if (still != null) {
            // fallback: delete by auction + bidder to be resilient to id/param mismatch from UI.
            autoBidDAO.deleteAutoBidByAuctionBidder(item.getAuctionId(), bidderId);
        }
        if (auctionService != null) {
            // Recompute auto bids in the auction since removing a rule may change the winner.
            auctionService.triggerAutoBids(item.getAuctionId());
        }
    }

    public List<AutoBid> getAutobidByBidder(int bidderId) {
        return autoBidDAO.findAllUserAutoBid(bidderId);
    }
    public AutoBid getAutobid(int id) {
        return autoBidDAO.findAutoBidById(id);
    }
}
