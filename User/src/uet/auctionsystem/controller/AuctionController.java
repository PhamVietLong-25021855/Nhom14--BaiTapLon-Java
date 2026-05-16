package uet.auctionsystem.controller;

import uet.auctionsystem.exception.AuctionClosedException;
import uet.auctionsystem.exception.InvalidBidException;
import uet.auctionsystem.exception.ItemNotFoundException;
import uet.auctionsystem.exception.UnauthorizedException;
import uet.auctionsystem.exception.ValidationException;
import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.BidTransaction;
import uet.auctionsystem.model.Role;
import uet.auctionsystem.model.User;
import uet.auctionsystem.service.AuctionService;
import java.util.List;
import java.util.Map;

// Ghi chu file: File controller nam giua giao dien va service; nhan lenh tu UI va goi nghiep vu tuong ung.
// Khai bao lop AuctionController; dieu phoi thao tac UI va chuyen tiep yeu cau xu ly nghiep vu.
public class AuctionController {
    // Thuoc tinh: giu tham chieu den AuctionService de phoi hop xu ly.
    private final AuctionService auctionService;

    // Ham tao: khoi tao doi tuong AuctionController voi cac phu thuoc can thiet.
    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    // Giá»¯ chá»¯ kÃ½ cÅ© Ä‘á»ƒ cÃ¡c mÃ n hÃ¬nh cÅ© váº«n gá»i Ä‘Æ°á»£c.
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create auction.
    public String createAuction(String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, int sellerId) {
        return createAuction(name, desc, startPrice, startTime, endTime, category, imageSource, null, sellerId);
    }

    // Chá»¯ kÃ½ má»›i cho seller upload áº£nh dáº¡ng byte[].
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create auction.
    public String createAuction(String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, byte[] imageData, int sellerId) {
        try {
            auctionService.createAuction(name, desc, startPrice, startTime, endTime, category, imageSource, imageData, sellerId);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    // Giá»¯ chá»¯ kÃ½ cÅ© Ä‘á»ƒ khÃ´ng lÃ m gÃ£y cÃ¡c Ä‘iá»ƒm gá»i hiá»‡n há»¯u.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction.
    public String updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource) {
        return updateAuction(auctionId, sellerId, name, desc, startPrice, startTime, endTime, category, imageSource, null);
    }

    // Chá»¯ kÃ½ má»›i cho update cÃ³ kÃ¨m áº£nh binary.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction.
    public String updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, byte[] imageData) {
        try {
            auctionService.updateAuction(auctionId, sellerId, name, desc, startPrice, startTime, endTime, category, imageSource, imageData);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete auction.
    public String deleteAuction(int auctionId, int sellerId) {
        try {
            auctionService.deleteAuction(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    // Phuong thuc: lay hoac doc du lieu cho thao tac get auctions by seller.
    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        return auctionService.getAuctionsBySeller(sellerId);
    }

    // Phuong thuc: lay hoac doc du lieu cho thao tac get all auctions.
    public List<AuctionItem> getAllAuctions() {
        return auctionService.getAllAuctions();
    }

    // Phuong thuc: lay hoac doc du lieu cho thao tac get bids for auction.
    public List<BidTransaction> getBidsForAuction(int auctionId) {
        return auctionService.getBidsForAuction(auctionId);
    }

    // Phuong thuc: lay hoac doc du lieu cho thao tac get all bids.
    public List<BidTransaction> getAllBids() {
        return auctionService.getAllBids();
    }

    // Phuong thuc: xu ly nghiep vu chinh cho thao tac place bid.
    public String placeBid(int auctionId, int bidderId, double amount) {
        try {
            auctionService.placeBid(auctionId, bidderId, amount);
            return "SUCCESS";
        } catch (ItemNotFoundException | AuctionClosedException | InvalidBidException e) {
            return e.getMessage();
        }
    }

    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac close auction.
    public String closeAuction(int auctionId, int sellerId) {
        try {
            auctionService.closeAuctionManually(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | AuctionClosedException e) {
            return e.getMessage();
        }
    }

    // Seller xÃ¡c nháº­n capture káº¿t quáº£ cuá»‘i cÃ¹ng.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac mark auction as paid.
    public String markAuctionAsPaid(int auctionId, int sellerId) {
        try {
            auctionService.markAuctionAsPaid(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    // Seller há»§y káº¿t quáº£ Ä‘Ã£ chá»‘t náº¿u cáº§n hoÃ n tÃ¡c settlement.
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac cancel finished auction.
    public String cancelFinishedAuction(int auctionId, int sellerId) {
        try {
            auctionService.cancelFinishedAuction(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }
}
