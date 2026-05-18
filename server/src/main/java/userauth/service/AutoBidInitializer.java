package userauth.service;

import userauth.dao.AutoBidDAO;
import userauth.dao.AuctionDAO;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;

import java.util.List;

/**
 * Helper service that creates default auto-bid rules for a newly registered user.
 * This is intentionally conservative: it only creates rules for OPEN/RUNNING auctions
 * where the user is not the seller and no existing auto-bid exists for (auction,user).
 */
public final class AutoBidInitializer {
    private final AutoBidDAO autoBidDAO;
    private final AuctionDAO auctionDAO;

    public AutoBidInitializer(AutoBidDAO autoBidDAO, AuctionDAO auctionDAO) {
        this.autoBidDAO = autoBidDAO;
        this.auctionDAO = auctionDAO;
    }

    /**
     * Create default auto-bid rules for the given user across active auctions.
     * Defaults: maxPrice = max(currentHighestBid + 1000, startPrice + 1000)
     *           increment = max(1, round(startPrice * 0.05))
     */
    public void createDefaultsForUser(int userId) {
        long now = System.currentTimeMillis();
        List<AuctionItem> auctions = auctionDAO.findAllAuctions();
        for (AuctionItem item : auctions) {
            if (item == null) continue;
            if (item.getSellerId() == userId) continue;
            AuctionStatus status = item.getStatus();
            if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED || status == AuctionStatus.PAID) {
                continue;
            }

            // skip if user already has auto-bid for this auction
            if (autoBidDAO.findAutoBidByAuctionBidder(item.getId(), userId) != null) {
                continue;
            }

            double current = item.getCurrentHighestBid();
            double start = item.getStartPrice();
            double defaultMax = Math.max(current + 1000.0, start + 1000.0);
            double defaultIncrement = Math.max(1.0, Math.round(start * 0.05));

            AutoBid autoBid = new AutoBid(0, item.getId(), userId, defaultMax, defaultIncrement, now, now);
            autoBidDAO.saveAutoBid(autoBid);
        }
    }
}
