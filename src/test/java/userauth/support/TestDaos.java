package userauth.support;

import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.dao.HomepageAnnouncementDAO;
import userauth.dao.UserDAO;
import userauth.model.AuctionItem;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;
import userauth.model.HomepageAnnouncement;
import userauth.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestDaos {
    private TestDaos() {
    }

    public static final class InMemoryUserDao implements UserDAO {
        private final AtomicInteger ids = new AtomicInteger(1);
        private final List<User> users = new CopyOnWriteArrayList<>();

        @Override
        public void save(User user) {
            if (user.getId() <= 0) {
                user.setId(ids.getAndIncrement());
            }
            users.add(user);
        }

        @Override
        public void update(User user) {
            deleteById(user.getId());
            users.add(user);
        }

        @Override
        public void deleteById(int userId) {
            users.removeIf(user -> user.getId() == userId);
        }

        @Override
        public User findByUsername(String username) {
            return users.stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findByEmail(String email) {
            return users.stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users);
        }
    }

    public static final class InMemoryAuctionDao implements AuctionDAO {
        private final AtomicInteger auctionIds = new AtomicInteger(1);
        private final AtomicInteger bidIds = new AtomicInteger(1);
        private final Map<Integer, AuctionItem> auctions = new ConcurrentHashMap<>();
        private final List<BidTransaction> bids = new CopyOnWriteArrayList<>();

        @Override
        public void saveAuction(AuctionItem item) {
            if (item.getId() <= 0) {
                item.setId(auctionIds.getAndIncrement());
            }
            auctions.put(item.getId(), item);
        }

        @Override
        public void updateAuction(AuctionItem item) {
            auctions.put(item.getId(), item);
        }

        @Override
        public void deleteAuction(int id) {
            auctions.remove(id);
            bids.removeIf(bid -> bid.getAuctionId() == id);
        }

        @Override
        public AuctionItem findAuctionById(int id) {
            return auctions.get(id);
        }

        @Override
        public List<AuctionItem> findAllAuctions() {
            return auctions.values().stream()
                    .sorted(Comparator.comparingInt(AuctionItem::getId))
                    .toList();
        }

        @Override
        public void saveBid(BidTransaction bid) {
            if (bid.getId() <= 0) {
                bid.setId(bidIds.getAndIncrement());
            }
            bids.add(bid);
        }

        @Override
        public List<BidTransaction> findAllBids() {
            return bids.stream()
                    .sorted(Comparator.comparingInt(BidTransaction::getId))
                    .toList();
        }

        @Override
        public List<BidTransaction> findBidsByAuction(int auctionId) {
            return bids.stream()
                    .filter(bid -> bid.getAuctionId() == auctionId)
                    .sorted(Comparator.comparingInt(BidTransaction::getId))
                    .toList();
        }
    }

    public static final class InMemoryAutoBidDao implements AutoBidDAO {
        private final AtomicInteger ids = new AtomicInteger(1);
        private final Map<Integer, AutoBid> autoBids = new ConcurrentHashMap<>();

        @Override
        public void saveAutoBid(AutoBid item) {
            if (item.getId() <= 0) {
                item.setId(ids.getAndIncrement());
            }
            autoBids.put(item.getId(), item);
        }

        @Override
        public void updateAutoBid(AutoBid item) {
            autoBids.put(item.getId(), item);
        }

        @Override
        public void deleteAutoBid(int id) {
            autoBids.remove(id);
        }

        @Override
        public AutoBid findAutoBidById(int id) {
            return autoBids.get(id);
        }

        @Override
        public AutoBid findAutoBidByAuctionBidder(int auctionId, int bidderId) {
            return autoBids.values().stream()
                    .filter(autoBid -> autoBid.getAuctionId() == auctionId && autoBid.getBidderId() == bidderId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<AutoBid> findAutoBidsByAuction(int auctionId) {
            return autoBids.values().stream()
                    .filter(autoBid -> autoBid.getAuctionId() == auctionId)
                    .sorted(Comparator.comparingInt(AutoBid::getId))
                    .toList();
        }

        @Override
        public List<AutoBid> findAllUserAutoBid(int bidderId) {
            return autoBids.values().stream()
                    .filter(autoBid -> autoBid.getBidderId() == bidderId)
                    .sorted(Comparator.comparingInt(AutoBid::getId))
                    .toList();
        }
    }

    public static final class InMemoryHomepageAnnouncementDao implements HomepageAnnouncementDAO {
        private final AtomicInteger ids = new AtomicInteger(1);
        private final Map<Integer, HomepageAnnouncement> announcements = new ConcurrentHashMap<>();

        @Override
        public void save(HomepageAnnouncement announcement) {
            if (announcement.getId() <= 0) {
                announcement.setId(ids.getAndIncrement());
            }
            announcements.put(announcement.getId(), announcement);
        }

        @Override
        public void update(HomepageAnnouncement announcement) {
            announcements.put(announcement.getId(), announcement);
        }

        @Override
        public void delete(int id) {
            announcements.remove(id);
        }

        @Override
        public HomepageAnnouncement findById(int id) {
            return announcements.get(id);
        }

        @Override
        public List<HomepageAnnouncement> findAll() {
            return announcements.values().stream()
                    .sorted(Comparator.comparingInt(HomepageAnnouncement::getId))
                    .toList();
        }
    }
}
