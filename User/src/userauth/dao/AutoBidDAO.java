package userauth.dao;

import userauth.model.AutoBid;
import java.util.List;

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao giao dien AutoBidDAO; phu trach hop dong hoac truy cap du lieu cho database.
public interface AutoBidDAO {
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save auto bid.
    void saveAutoBid(AutoBid item);
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auto bid.
    void updateAutoBid(AutoBid item);
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete auto bid.
    void deleteAutoBid(int id);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find auto bid by id.
    AutoBid findAutoBidById(int id);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find auto bid by auction bidder.
    AutoBid findAutoBidByAuctionBidder(int auctionId, int bidderId);

    // DÃ¹ng cho logic xá»­ lÃ½ auto-bid hoáº·c kiá»ƒm tra luáº­t Ä‘ang tá»“n táº¡i trÃªn má»™t auction.
    // Phuong thuc: lay hoac doc du lieu cho thao tac find auto bids by auction.
    List<AutoBid> findAutoBidsByAuction(int auctionId);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all user auto bid.
    List<AutoBid> findAllUserAutoBid(int bidderId);
}
