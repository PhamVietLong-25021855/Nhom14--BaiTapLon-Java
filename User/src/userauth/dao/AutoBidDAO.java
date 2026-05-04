package userauth.dao;

import userauth.model.AutoBid;

import java.util.List;

public interface AutoBidDAO {
    void saveAutoBid(AutoBid item);

    void updateAutoBid(AutoBid item);

    void deleteAutoBid(int id);

    AutoBid findAutoBidById(int id);

    AutoBid findAutoBidByAuctionBidder(int auctionId, int bidderId);

    List<AutoBid> findAllUserAutoBid(int bidderId);
}
