package userauth.dao;

import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import java.util.List;

public interface AuctionDAO {
    void saveAuction(AuctionItem item);
    void updateAuction(AuctionItem item);
    void deleteAuction(int id);
    AuctionItem findAuctionById(int id);
    List<AuctionItem> findAllAuctions();
    
    void saveBid(BidTransaction bid);
    List<BidTransaction> findAllBids();
    List<BidTransaction> findBidsByAuction(int auctionId);
}
