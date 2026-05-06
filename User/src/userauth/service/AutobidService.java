package userauth.service;

import userauth.dao.AutoBidDAO;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;
import java.util.List;

// Ghi chu file: File service; chua nghiep vu chinh va phoi hop giua controller, DAO va event.
// Khai bao lop AutobidService; chua xu ly nghiep vu va cac quy tac chinh cua he thong.
public class AutobidService {
    // Thuoc tinh: giu tham chieu den AutoBidDAO de phoi hop xu ly.
    private final AutoBidDAO autoBidDAO;
    // Ham tao: khoi tao doi tuong AutobidService voi cac phu thuoc can thiet.
    public AutobidService(AutoBidDAO autoBidDAO) {
        this.autoBidDAO = autoBidDAO;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create autobid.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update autobid.
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
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete autobid.
    public void deleteAutobid(int bidderId, int id) throws ItemNotFoundException, UnauthorizedException {
        requireOwnedAutobid(bidderId, id);
        autoBidDAO.deleteAutoBid(id);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get autobid by bidder.
    public List<AutoBid> getAutobidByBidder(int bidderId) {
        return autoBidDAO.findAllUserAutoBid(bidderId);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get autobid.
    public AutoBid getAutobid(int id) {
        return autoBidDAO.findAutoBidById(id);
    }
    // Phuong thuc: thuc hien chuc nang require owned autobid trong lop AutobidService.
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
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac validate thresholds.
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
