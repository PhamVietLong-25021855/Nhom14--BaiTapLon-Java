package userauth.dao;

import userauth.model.AuctionItem;
import userauth.model.AutoBid;

import java.util.List;

public interface AutoBidDAO {
    void saveAutoBid(AutoBid item);
    void updateAutoBid(AutoBid item);
    void deleteAutoBid(int id);
    /**
     * Delete auto-bid record by auction_id and bidder_id. Some clients may pass incorrect ids;
     * this provides a resilient deletion fallback.
     */
    void deleteAutoBidByAuctionBidder(int auctionId, int bidderId);
    AutoBid findAutoBidById(int id);
    AutoBid findAutoBidByAuctionBidder(int auction_id, int bidder_id);
    List<AutoBid> findAutoBidsByAuction(int auctionId);
    List<AutoBid> findAllUserAutoBid(int bidderId);
}
