package userauth.dao;

import userauth.model.AuctionItem;
import userauth.model.AutoBid;

import java.util.List;

public interface AutoBidDAO {
    void saveAutoBid(AutoBid item);
    void updateAutoBid(AutoBid item);
    void deleteAutoBid(int id);
    AutoBid findAutoBidById(int id);
    AutoBid findAutoBidByAuctionBidder(int auction_id, int bidder_id);
    List<AutoBid> findAllUserAutoBid(int bidderId);
    List<AutoBid> findAutoBidsByAuction(int auctionId);
}
