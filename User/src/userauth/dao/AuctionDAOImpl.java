package userauth.dao;

import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.service.AuctionFileService;

import java.util.ArrayList;
import java.util.List;

public class AuctionDAOImpl implements AuctionDAO {
    private final List<AuctionItem> auctions;
    private final List<BidTransaction> bids;
    private final AuctionFileService fileService;

    public AuctionDAOImpl() {
        fileService = new AuctionFileService();
        auctions = new ArrayList<>(fileService.loadAuctionsFromFile());
        bids = new ArrayList<>(fileService.loadBidsFromFile());
    }

    @Override
    public void saveAuction(AuctionItem item) {
        if (findAuctionById(item.getId()) != null) {
            return;
        }

        auctions.add(item);
        persistAuctions();
    }

    @Override
    public void updateAuction(AuctionItem item) {
        persistAuctions();
    }

    @Override
    public void deleteAuction(int id) {
        auctions.removeIf(auction -> auction.getId() == id);
        persistAuctions();
    }

    @Override
    public AuctionItem findAuctionById(int id) {
        for (AuctionItem auction : auctions) {
            if (auction.getId() == id) {
                return auction;
            }
        }
        return null;
    }

    @Override
    public List<AuctionItem> findAllAuctions() {
        return new ArrayList<>(auctions);
    }

    @Override
    public void saveBid(BidTransaction bid) {
        bids.add(bid);
        fileService.saveBidsToFile(bids);
    }

    @Override
    public List<BidTransaction> findBidsByAuction(int auctionId) {
        List<BidTransaction> result = new ArrayList<>();
        for (BidTransaction bid : bids) {
            if (bid.getAuctionId() == auctionId) {
                result.add(bid);
            }
        }
        return result;
    }

    private void persistAuctions() {
        fileService.saveAuctionsToFile(auctions);
    }
}
