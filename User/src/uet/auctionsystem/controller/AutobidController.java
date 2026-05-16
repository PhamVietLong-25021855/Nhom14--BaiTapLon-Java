package uet.auctionsystem.controller;

import uet.auctionsystem.exception.ItemNotFoundException;
import uet.auctionsystem.exception.UnauthorizedException;
import uet.auctionsystem.exception.ValidationException;
import uet.auctionsystem.model.AutoBid;
import uet.auctionsystem.service.AutobidService;
import java.util.List;

// Ghi chu file: File controller nam giua giao dien va service; nhan lenh tu UI va goi nghiep vu tuong ung.
// Khai bao lop AutobidController; dieu phoi thao tac UI va chuyen tiep yeu cau xu ly nghiep vu.
public class AutobidController {
    // Thuoc tinh: giu tham chieu den AutobidService de phoi hop xu ly.
    private final AutobidService autobidService;
    // Ham tao: khoi tao doi tuong AutobidController voi cac phu thuoc can thiet.
    public AutobidController(AutobidService autobidService) {
        this.autobidService = autobidService;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create autobid.
    public String createAutobid(int bidderId, int auctionId, double maxPrice, double increment) {
        try {
            autobidService.createAutobid(bidderId, auctionId, maxPrice, increment);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update autobid.
    public String updateAutobid(int bidderId, int id, double maxPrice, double increment) {
        try {
            autobidService.updateAutobid(bidderId, id, maxPrice, increment);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete auto bid.
    public String deleteAutoBid(int bidderId, int id) {
        try {
            autobidService.deleteAutobid(bidderId, id);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException e) {
            return e.getMessage();
        }
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get autobid by bidder.
    public List<AutoBid> getAutobidByBidder(int bidderId) {
        return autobidService.getAutobidByBidder(bidderId);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get autobid by id.
    public AutoBid getAutobidById(int id) {
        return autobidService.getAutobid(id);
    }
}
