package userauth.service;

import userauth.dao.AutoBidDAO;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;

import java.util.List;

// File note: Tầng nghiệp vụ cho auto-bid; validate input và kiểm tra quyền sở hữu.
// Táº§ng nghiá»‡p vá»¥ cho auto-bid: validate input vÃ  kiá»ƒm tra quyá»n sá»Ÿ há»¯u.
public class AutobidService {
    private final AutoBidDAO autoBidDAO;

    public AutobidService(AutoBidDAO autoBidDAO) {
        this.autoBidDAO = autoBidDAO;
    }

    public void createAutobid(int bidderId, int auctionId, double maxPrice, double increment)
            throws ValidationException {
        validateThresholds(maxPrice, increment);
        if (autoBidDAO.findAutoBidByAuctionBidder(auctionId, bidderId) != null) {
            throw new ValidationException("Already existed autobid for this auction");
        }

        // Khi táº¡o má»›i thÃ¬ createdAt vÃ  updatedAt khá»Ÿi táº¡o cÃ¹ng lÃºc.
        long now = System.currentTimeMillis();
        AutoBid item = new AutoBid(0, auctionId, bidderId, maxPrice, increment, now, now);
        autoBidDAO.saveAutoBid(item);
    }

    public void updateAutobid(int bidderId, int id, double maxPrice, double increment)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        AutoBid item = requireOwnedAutobid(bidderId, id);
        validateThresholds(maxPrice, increment);
        item.setMaxPrice(maxPrice);
        item.setIncrement(increment);
        // Giá»¯ dáº¥u váº¿t láº§n chá»‰nh sá»­a cuá»‘i Ä‘á»ƒ sort á»•n Ä‘á»‹nh vÃ  debug dá»… hÆ¡n.
        item.setUpdatedAt(System.currentTimeMillis());
        autoBidDAO.updateAutoBid(item);
    }

    public void deleteAutobid(int bidderId, int id) throws ItemNotFoundException, UnauthorizedException {
        requireOwnedAutobid(bidderId, id);
        autoBidDAO.deleteAutoBid(id);
    }

    public List<AutoBid> getAutobidByBidder(int bidderId) {
        return autoBidDAO.findAllUserAutoBid(bidderId);
    }

    public AutoBid getAutobid(int id) {
        return autoBidDAO.findAutoBidById(id);
    }

    private AutoBid requireOwnedAutobid(int bidderId, int id) throws ItemNotFoundException, UnauthorizedException {
        AutoBid item = autoBidDAO.findAutoBidById(id);
        if (item == null) {
            throw new ItemNotFoundException("AutoBid item not found.");
        }
        if (item.getBidderId() != bidderId) {
            throw new UnauthorizedException("Only the creator can edit this item.");
        }
        return item;
    }

    private void validateThresholds(double maxPrice, double increment) throws ValidationException {
        if (maxPrice <= 0) {
            throw new ValidationException("Max price must be greater than 0.");
        }
        if (increment <= 0) {
            throw new ValidationException("Increment must be greater than 0.");
        }
        if (increment > maxPrice) {
            throw new ValidationException("Increment cannot be greater than max price.");
        }
    }
}

