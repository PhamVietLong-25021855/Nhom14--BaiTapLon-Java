package userauth.dao;

import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import java.util.List;
import java.util.Map;

public interface AuctionDAO {
    void saveAuction(AuctionItem item);
    void updateAuction(AuctionItem item);
    void updateAuctionState(AuctionItem item);
    void deleteAuction(int id);
    AuctionItem findAuctionById(int id);
    List<AuctionItem> findAllAuctions();
    List<AuctionItem> findAllAuctionSummaries();
    List<AuctionItem> findAuctionsBySeller(int sellerId);
    List<AuctionItem> findStatusRefreshCandidates(long now);
    List<Integer> findAllAuctionIds();
    List<AuctionItem> findFinishedAuctions();
    List<AuctionItem> findAuctionsHoldingReservedFunds();
    
    void saveBid(BidTransaction bid);
    List<BidTransaction> findAllBids();
    List<BidTransaction> findBidsByAuction(int auctionId);
    Map<Integer, Integer> findBidCounts();
    int countAllBids();
}
