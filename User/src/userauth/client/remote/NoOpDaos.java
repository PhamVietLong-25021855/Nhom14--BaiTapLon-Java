package userauth.client.remote;

import userauth.dao.*;
import userauth.model.*;

import java.util.List;

/** DAO giả chỉ dùng để thỏa constructor của Service remote; client không được truy cập database. */
final class NoOpDaos {
    private NoOpDaos() {}

    static final class UserDao implements UserDAO {
        public void save(User user) { throw unsupported(); }
        public void update(User user) { throw unsupported(); }
        public void deleteById(int userId) { throw unsupported(); }
        public User findByUsername(String username) { throw unsupported(); }
        public User findByEmail(String email) { throw unsupported(); }
        public List<User> findAll() { throw unsupported(); }
    }

    static final class AuctionDao implements AuctionDAO {
        public void saveAuction(AuctionItem item) { throw unsupported(); }
        public void updateAuction(AuctionItem item) { throw unsupported(); }
        public void deleteAuction(int id) { throw unsupported(); }
        public AuctionItem findAuctionById(int id) { throw unsupported(); }
        public List<AuctionItem> findAllAuctions() { throw unsupported(); }
        public void saveBid(BidTransaction bid) { throw unsupported(); }
        public List<BidTransaction> findAllBids() { throw unsupported(); }
        public List<BidTransaction> findBidsByAuction(int auctionId) { throw unsupported(); }
    }

    static final class AutoBidDao implements AutoBidDAO {
        public void saveAutoBid(AutoBid item) { throw unsupported(); }
        public void updateAutoBid(AutoBid item) { throw unsupported(); }
        public void deleteAutoBid(int id) { throw unsupported(); }
        public AutoBid findAutoBidById(int id) { throw unsupported(); }
        public AutoBid findAutoBidByAuctionBidder(int auctionId, int bidderId) { throw unsupported(); }
        public List<AutoBid> findAutoBidsByAuction(int auctionId) { throw unsupported(); }
        public List<AutoBid> findAllUserAutoBid(int bidderId) { throw unsupported(); }
    }

    static final class HomepageDao implements HomepageAnnouncementDAO {
        public void save(HomepageAnnouncement announcement) { throw unsupported(); }
        public void update(HomepageAnnouncement announcement) { throw unsupported(); }
        public void delete(int id) { throw unsupported(); }
        public HomepageAnnouncement findById(int id) { throw unsupported(); }
        public List<HomepageAnnouncement> findAll() { throw unsupported(); }
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Client remote mode must call the server, not the database.");
    }
}
