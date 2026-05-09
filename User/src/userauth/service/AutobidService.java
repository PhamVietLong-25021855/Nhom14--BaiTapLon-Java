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

    public AutobidService(AutoBidDAO autoBidDAO) {
        this.autoBidDAO = autoBidDAO;
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
        if (autoBidDAO.findAutoBidByAuctionBidder(auctionId,bidderId) != null){
            throw new ValidationException("Already existed autobid for this auction");
        }
        long now = System.currentTimeMillis();
        AutoBid item = new AutoBid(0, auctionId, bidderId, maxPrice, increment, now, now);
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
}
