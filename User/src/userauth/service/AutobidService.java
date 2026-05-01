package userauth.service;

import userauth.dao.AutoBidDAO;
import userauth.exception.*;
import userauth.model.AutoBid;

import java.util.List;

public class AutobidService {

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
        if (autoBidDAO.findAutoBidByAuctionBidder(auctionId,bidderId) != null){
            throw new ValidationException("Already existed autobid for this auction");
        }
        AutoBid item = new AutoBid(0, auctionId, bidderId,maxPrice,increment);
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
}
