package userauth.service;

import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class AuctionService {
    private static final int ADMIN_EARLY_CLOSE_COUNTS = 3;

    private final AuctionDAO auctionDAO;
    private final AutoBidDAO autoBidDAO ;
    private final WalletService walletService ;
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks;
    private final ConcurrentHashMap<Integer, AdminEarlyCloseState> adminEarlyCloseStates;

    public AuctionService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO, WalletService WalletService) {
        this.auctionDAO = auctionDAO;
        this.autoBidDAO = autoBidDAO;
        this.walletService = WalletService ;
        this.auctionLocks = new ConcurrentHashMap<>();
        this.adminEarlyCloseStates = new ConcurrentHashMap<>();
    }

    private ReentrantLock getLockForAuction(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, ignored -> new ReentrantLock());
    }

    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, int sellerId)
            throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Product name cannot be empty.");
        }
        if (startPrice <= 0) {
            throw new ValidationException("Starting price must be greater than 0.");
        }
        if (startTime >= endTime) {
            throw new ValidationException("Start time must be earlier than end time.");
        }
        if (endTime <= System.currentTimeMillis()) {
            throw new ValidationException("Cannot create an expired auction.");
        }

        AuctionItem item = new AuctionItem(0, name, desc, startPrice, startTime, endTime, category, normalizeOptionalText(imageSource), sellerId);
        auctionDAO.saveAuction(item);
    }

    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) {
            throw new ItemNotFoundException("Auction item not found.");
        }
        if (item.getSellerId() != sellerId) {
            throw new UnauthorizedException("Only the creator can edit this item.");
        }

        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        if (!bids.isEmpty()) {
            throw new ValidationException("This item already has bids and can no longer be edited.");
        }
        if (item.getStatus() == AuctionStatus.RUNNING || item.getStatus() == AuctionStatus.FINISHED) {
            throw new ValidationException("This item can only be edited before it starts or while it is in OPEN status.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Product name cannot be empty.");
        }
        if (startPrice <= 0) {
            throw new ValidationException("Starting price must be greater than 0.");
        }
        if (startTime >= endTime) {
            throw new ValidationException("Start time must be earlier than end time.");
        }

        item.setName(name);
        item.setDescription(desc);
        item.setStartPrice(startPrice);
        item.setCurrentHighestBid(startPrice);
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setCategory(category);
        item.setImageSource(normalizeOptionalText(imageSource));
        item.setUpdatedAt(System.currentTimeMillis());
        auctionDAO.updateAuction(item);
    }

    public void deleteAuction(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) {
            throw new ItemNotFoundException("Auction item not found.");
        }
        if (item.getSellerId() != sellerId) {
            throw new UnauthorizedException("Only the creator can delete or cancel this item.");
        }

        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        if (bids.isEmpty()) {
            auctionDAO.deleteAuction(auctionId);
        } else {
            item.setStatus(AuctionStatus.CANCELED);
            item.setUpdatedAt(System.currentTimeMillis());
            auctionDAO.updateAuction(item);
        }
        adminEarlyCloseStates.remove(auctionId);
    }

    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        return auctionDAO.findAllAuctions().stream()
                .filter(item -> item.getSellerId() == sellerId)
                .collect(Collectors.toList());
    }

    public List<AuctionItem> getAllAuctions() {

        return auctionDAO.findAllAuctions();
    }

    public List<BidTransaction> getBidsForAuction(int auctionId) {
        return auctionDAO.findBidsByAuction(auctionId);
    }

    public List<BidTransaction> getAllBids() {
        return auctionDAO.findAllBids();
    }

    public void placeBid(int auctionId, int bidderId, double amount)
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) {
                throw new ItemNotFoundException("Auction item not found.");
            }
            if (item.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("The auction is not currently running.");
            }

            long now = System.currentTimeMillis();
            if (now < item.getStartTime() || now > item.getEndTime()) {
                throw new AuctionClosedException("The current time is not valid for bidding.");
            }
            if (amount <= item.getStartPrice()) {
                throw new InvalidBidException("The amount must be higher than the starting price (" + item.getStartPrice() + ").");
            }
            if (amount <= item.getCurrentHighestBid()) {
                throw new InvalidBidException("The amount must be higher than the current price (" + item.getCurrentHighestBid() + ").");
            }
            // 1.Kiểm tra số dư ví người đặt giá thu công :
            userauth.model.Wallet bidderWallet = walletService.getWallet(bidderId);
            if(bidderWallet == null || bidderWallet.getBalance() < amount){
                throw new InvalidBidException("Số dư trong ví (" + userauth.gui.fxml.AuctionViewFormatter.formatMoney(bidderWallet != null ? bidderWallet.getBalance() : 0) + ") không đủ để đặt mức giá này.");
            }

            // Lưu lenh bid :
            auctionDAO.saveBid(new BidTransaction(0, auctionId, bidderId, amount, now, "ACCEPTED"));
            item.setCurrentHighestBid(amount);
            item.setWinnerId(bidderId);
            item.setUpdatedAt(now);
            auctionDAO.updateAuction(item);

            boolean autoBidTriggered;
            do {
                autoBidTriggered = false;
                List<userauth.model.AutoBid> activeRobots = autoBidDAO.findAutoBidsByAuction(auctionId);
                userauth.model.AutoBid nextAttacker = null;
                double currentPrice = item.getCurrentHighestBid();

                for (userauth.model.AutoBid robot : activeRobots) {
                    if (robot.getBidderId() == item.getWinnerId()) continue;

                    double requiredBid = currentPrice + robot.getIncrement();

                    if (requiredBid <= robot.getMaxPrice()) {
                        // THÊM KIỂM TRA VÍ CHO ROBOT: Robot chỉ được đánh nếu chủ nó còn tiền!
                        userauth.model.Wallet robotWallet = walletService.getWallet(robot.getBidderId());
                        if (robotWallet != null && robotWallet.getBalance() >= requiredBid) {
                            if (nextAttacker == null) {
                                nextAttacker = robot;
                            } else if (robot.getMaxPrice() > nextAttacker.getMaxPrice()) {
                                nextAttacker = robot;
                            } else if (robot.getMaxPrice() == nextAttacker.getMaxPrice() && robot.getId() < nextAttacker.getId()) {
                                nextAttacker = robot;
                            }
                        }
                    }
                }

                if (nextAttacker != null) {
                    double autoBidAmount = currentPrice + nextAttacker.getIncrement();
                    long autoNow = System.currentTimeMillis();

                    auctionDAO.saveBid(new BidTransaction(0, auctionId, nextAttacker.getBidderId(), autoBidAmount, autoNow, "AUTO_BID"));
                    item.setCurrentHighestBid(autoBidAmount);
                    item.setWinnerId(nextAttacker.getBidderId());
                    item.setUpdatedAt(autoNow);
                    auctionDAO.updateAuction(item);
                    autoBidTriggered = true;
                }
            } while (autoBidTriggered);

            refreshEarlyCloseSnapshot(auctionId, item, System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }

    public void closeAuctionManually(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, AuctionClosedException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) {
            throw new ItemNotFoundException("Item not found.");
        }
        if (item.getSellerId() != sellerId) {
            throw new UnauthorizedException("You do not have permission to close this auction.");
        }
        if (item.getStatus() == AuctionStatus.FINISHED ||
                item.getStatus() == AuctionStatus.CANCELED ||
                item.getStatus() == AuctionStatus.PAID) {
            throw new AuctionClosedException("The auction has already ended or was cancelled.");
        }

        item.setStatus(AuctionStatus.FINISHED);
        item.setEndTime(System.currentTimeMillis());
        item.setUpdatedAt(System.currentTimeMillis());
        auctionDAO.updateAuction(item);
        adminEarlyCloseStates.remove(auctionId);
    }

    public void startAdminEarlyCloseCountdown(int auctionId)
            throws ItemNotFoundException, AuctionClosedException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) {
                throw new ItemNotFoundException("Auction not found.");
            }
            if (item.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Early-close countdown is only available while the auction is RUNNING.");
            }
            if (adminEarlyCloseStates.containsKey(auctionId)) {
                throw new ValidationException("This auction is already in an early-close countdown process.");
            }

            List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
            adminEarlyCloseStates.put(auctionId, AdminEarlyCloseState.from(item, bids, System.currentTimeMillis()));
        } finally {
            lock.unlock();
        }
    }

    public void cancelAdminEarlyCloseCountdown(int auctionId) throws ItemNotFoundException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) {
                throw new ItemNotFoundException("Auction not found.");
            }
            if (adminEarlyCloseStates.remove(auctionId) == null) {
                throw new ValidationException("This auction has not activated the early-close countdown.");
            }
        } finally {
            lock.unlock();
        }
    }

    public Map<Integer, Integer> getAdminEarlyCloseCountdowns() {
        Map<Integer, Integer> countdowns = new HashMap<>();
        adminEarlyCloseStates.forEach((auctionId, state) -> countdowns.put(auctionId, state.remainingCounts));
        return countdowns;
    }

    public void refreshAuctionStatuses() {
        long now = System.currentTimeMillis();
        for (AuctionItem item : auctionDAO.findAllAuctions()) {
            AuctionStatus currentStatus = item.getStatus();

            if (currentStatus == AuctionStatus.FINISHED ||
                    currentStatus == AuctionStatus.PAID ||
                    currentStatus == AuctionStatus.CANCELED) {
                adminEarlyCloseStates.remove(item.getId());
                continue;
            }

            if (currentStatus == AuctionStatus.OPEN &&
                    now >= item.getStartTime() &&
                    now < item.getEndTime()) {
                item.setStatus(AuctionStatus.RUNNING);
                item.setUpdatedAt(now);
                auctionDAO.updateAuction(item);
                continue;
            }

            if ((currentStatus == AuctionStatus.OPEN || currentStatus == AuctionStatus.RUNNING) &&
                    now >= item.getEndTime()) {
                processAuctionClosing(item, now);
            }
        }

        tickAdminEarlyCloseCountdowns(now);
    }

    private void tickAdminEarlyCloseCountdowns(long now) {
        for (Map.Entry<Integer, AdminEarlyCloseState> entry : new HashMap<>(adminEarlyCloseStates).entrySet()) {
            int auctionId = entry.getKey();
            ReentrantLock lock = getLockForAuction(auctionId);
            lock.lock();
            try {
                AuctionItem item = auctionDAO.findAuctionById(auctionId);
                AdminEarlyCloseState state = adminEarlyCloseStates.get(auctionId);

                if (item == null || state == null || item.getStatus() != AuctionStatus.RUNNING) {
                    adminEarlyCloseStates.remove(auctionId);
                    continue;
                }

                if (now - state.lastTickAt < 1000) {
                    continue;
                }

                List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
                long latestBidTimestamp = findLatestBidTimestamp(bids);
                if (bids.size() != state.observedBidCount ||
                        Double.compare(item.getCurrentHighestBid(), state.observedHighestBid) != 0 ||
                        latestBidTimestamp != state.observedLatestBidTimestamp) {
                    state.reset(bids.size(), item.getCurrentHighestBid(), latestBidTimestamp, now);
                    continue;
                }

                state.lastTickAt = now;
                state.remainingCounts--;
                if (state.remainingCounts <= 0) {
                    processAuctionClosing(item, now);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void refreshEarlyCloseSnapshot(int auctionId, AuctionItem item, long now) {
        AdminEarlyCloseState state = adminEarlyCloseStates.get(auctionId);
        if (state == null) {
            return;
        }

        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        state.reset(bids.size(), item.getCurrentHighestBid(), findLatestBidTimestamp(bids), now);
    }

    private long findLatestBidTimestamp(List<BidTransaction> bids) {
        long latestTimestamp = -1;
        for (BidTransaction bid : bids) {
            if (bid.getTimestamp() > latestTimestamp) {
                latestTimestamp = bid.getTimestamp();
            }
        }
        return latestTimestamp;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class AdminEarlyCloseState {
        private int remainingCounts;
        private int observedBidCount;
        private double observedHighestBid;
        private long observedLatestBidTimestamp;
        private long lastTickAt;

        private static AdminEarlyCloseState from(AuctionItem item, List<BidTransaction> bids, long now) {
            AdminEarlyCloseState state = new AdminEarlyCloseState();
            state.reset(bids.size(), item.getCurrentHighestBid(), latestTimestamp(bids), now);
            return state;
        }

        private void reset(int bidCount, double highestBid, long latestBidTimestamp, long now) {
            this.remainingCounts = ADMIN_EARLY_CLOSE_COUNTS;
            this.observedBidCount = bidCount;
            this.observedHighestBid = highestBid;
            this.observedLatestBidTimestamp = latestBidTimestamp;
            this.lastTickAt = now;
        }

        private static long latestTimestamp(List<BidTransaction> bids) {
            long latestTimestamp = -1;
            for (BidTransaction bid : bids) {
                if (bid.getTimestamp() > latestTimestamp) {
                    latestTimestamp = bid.getTimestamp();
                }
            }
            return latestTimestamp;
        }
    }

    // ham xu li chot phien va tru tien :
    private void processAuctionClosing(AuctionItem item, long now){
        item.setStatus(AuctionStatus.FINISHED);
        item.setEndTime(now);
        // neu co nguoi thang, tru tien tu vi nguoi thang va chuyen sang trang thai da thanh toan :
        if(item.getWinnerId() > 0){
            try{
                walletService.deductFromWallet(item.getWinnerId(), item.getCurrentHighestBid());
                item.setStatus(AuctionStatus.PAID); // doi thanh da thanh toan
            }catch (Exception e){
                System.out.println("ERROR processing payment for auction " + item.getId() + ": " + e.getMessage());
            }
        }
        item.setUpdatedAt(now);
        auctionDAO.updateAuction(item);
        adminEarlyCloseStates.remove(item.getId());
    }
}
