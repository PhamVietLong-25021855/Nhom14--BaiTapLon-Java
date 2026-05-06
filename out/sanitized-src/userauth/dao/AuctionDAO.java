package userauth.dao;

import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import java.util.List;

// File note: Interface DAO mô tả các thao tác truy cập dữ liệu của module này.
public interface AuctionDAO {
    void saveAuction(AuctionItem item);
    void updateAuction(AuctionItem item);
    void deleteAuction(int id);
    AuctionItem findAuctionById(int id);
    List<AuctionItem> findAllAuctions();
    
    void saveBid(BidTransaction bid);
    void saveBidAndUpdateAuction(BidTransaction bid, AuctionItem item);
    List<BidTransaction> findAllBids();
    List<BidTransaction> findBidsByAuction(int auctionId);
}

