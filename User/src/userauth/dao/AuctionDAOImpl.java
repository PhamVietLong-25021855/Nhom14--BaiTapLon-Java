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
        this.fileService = new AuctionFileService();
        this.auctions = new ArrayList<>(fileService.loadAuctionsFromFile());
        this.bids = new ArrayList<>(fileService.loadBidsFromFile());
    }

    @Override
    public void saveAuction(AuctionItem item) {
        if (findAuctionById(item.getId()) == null) {
            auctions.add(item);
            fileService.saveAuctionsToFile(auctions);
        }
    }

    @Override
    public void updateAuction(AuctionItem item) {
        // Dữ liệu đã lưu trong references List của bộ nhớ, ta chỉ cần xuất lại ra file
        fileService.saveAuctionsToFile(auctions);
    }

    @Override
    public AuctionItem findAuctionById(int id) {
        for (AuctionItem a : auctions) {
            if (a.getId() == id) return a;
        }
        return null;
    }

    @Override
    public void deleteAuction(int id) {
        auctions.removeIf(a -> a.getId() == id);
        fileService.saveAuctionsToFile(auctions);
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
        for (BidTransaction b : bids) {
            if (b.getAuctionId() == auctionId) {
                result.add(b);
            }
        }
        return result;
    }
}
