package userauth.dao;

import userauth.model.AutoBid;

import java.util.List;

// File note: Interface DAO mô tả các thao tác truy cập dữ liệu của module này.
// Contract truy cáº­p dá»¯ liá»‡u cho auto-bid.
public interface AutoBidDAO {
    void saveAutoBid(AutoBid item);

    void updateAutoBid(AutoBid item);

    void deleteAutoBid(int id);

    AutoBid findAutoBidById(int id);

    AutoBid findAutoBidByAuctionBidder(int auctionId, int bidderId);

    // DÃ¹ng cho logic xá»­ lÃ½ auto-bid hoáº·c kiá»ƒm tra luáº­t Ä‘ang tá»“n táº¡i trÃªn má»™t auction.
    List<AutoBid> findAutoBidsByAuction(int auctionId);

    List<AutoBid> findAllUserAutoBid(int bidderId);
}

