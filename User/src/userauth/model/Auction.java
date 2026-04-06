package userauth.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Auction: quản lý trung tâm một phiên đấu giá.
 * Chứa thông tin về item đấu giá và danh sách các giao dịch đặt giá (BidTransaction).
 * Cung cấp các method quản lý: thêm bid, lấy bid cao nhất, đóng phiên.
 */
public class Auction {
    private AuctionItem item;
    private List<BidTransaction> bidTransactions;

    public Auction(AuctionItem item) {
        this.item = item;
        this.bidTransactions = new ArrayList<>();
    }

    public Auction(AuctionItem item, List<BidTransaction> bidTransactions) {
        this.item = item;
        this.bidTransactions = bidTransactions != null ? bidTransactions : new ArrayList<>();
    }

    // Getter / Setter
    public AuctionItem getItem() { return item; }
    public void setItem(AuctionItem item) { this.item = item; }

    public List<BidTransaction> getBidTransactions() { return bidTransactions; }
    public void setBidTransactions(List<BidTransaction> bidTransactions) { this.bidTransactions = bidTransactions; }

    /**
     * Thêm một giao dịch đặt giá vào phiên đấu giá
     */
    public void addBidTransaction(BidTransaction bid) {
        bidTransactions.add(bid);
        if (bid.getAmount() > item.getCurrentHighestBid()) {
            item.setCurrentHighestBid(bid.getAmount());
            item.setWinnerId(bid.getBidderId());
        }
    }

    /**
     * Lấy giao dịch có giá cao nhất
     */
    public BidTransaction getHighestBid() {
        BidTransaction highest = null;
        for (BidTransaction bt : bidTransactions) {
            if (highest == null || bt.getAmount() > highest.getAmount()) {
                highest = bt;
            }
        }
        return highest;
    }

    /**
     * Lấy tổng số lượt đặt giá
     */
    public int getTotalBids() {
        return bidTransactions.size();
    }

    /**
     * Đóng phiên đấu giá
     */
    public void closeAuction() {
        item.setStatus(AuctionStatus.FINISHED);
        item.setEndTime(System.currentTimeMillis());
        item.setUpdatedAt(System.currentTimeMillis());
    }

    /**
     * Kiểm tra phiên còn hoạt động không
     */
    public boolean isActive() {
        return item.getStatus() == AuctionStatus.RUNNING;
    }

    @Override
    public String toString() {
        return "Auction{item=" + item.getName() + ", totalBids=" + bidTransactions.size() + ", status=" + item.getStatus() + "}";
    }
}
