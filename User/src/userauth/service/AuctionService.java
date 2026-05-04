package userauth.service;

import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.gui.fxml.AuctionViewFormatter;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidRequest;
import userauth.model.BidResult;
import userauth.model.BidTransaction;
import userauth.model.Wallet;
import userauth.realtime.AuctionEventPublisher;
import userauth.realtime.AuctionEventType;
import userauth.realtime.AuctionUpdateEvent;
import userauth.realtime.NoOpAuctionEventPublisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {
    private static final int ADMIN_EARLY_CLOSE_COUNTS = 3;
    private static final long EARLY_CLOSE_TICK_INTERVAL_MS = 1_000L;
    private static final long NO_BID_TIMESTAMP = -1L;

    private final AuctionDAO auctionDAO;
    private final AutoBidDAO autoBidDAO;
    private final WalletService walletService;
    private final AuctionLockManager auctionLockManager;
    private final AntiSnipingService antiSnipingService;
    private final AuctionEventPublisher auctionEventPublisher;
    private final ConcurrentHashMap<Integer, AdminEarlyCloseState> adminEarlyCloseStates;

    public AuctionService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO, WalletService walletService) {
        this(
                auctionDAO,
                autoBidDAO,
                walletService,
                new AuctionLockManager(),
                new AntiSnipingService(),
                new NoOpAuctionEventPublisher()
        );
    }

    public AuctionService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO, WalletService walletService,
                          AuctionLockManager auctionLockManager) {
        this(
                auctionDAO,
                autoBidDAO,
                walletService,
                auctionLockManager,
                new AntiSnipingService(),
                new NoOpAuctionEventPublisher()
        );
    }

    public AuctionService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO, WalletService walletService,
                          AuctionLockManager auctionLockManager, AntiSnipingService antiSnipingService,
                          AuctionEventPublisher auctionEventPublisher) {
        this.auctionDAO = auctionDAO;
        this.autoBidDAO = autoBidDAO;
        this.walletService = walletService;
        this.auctionLockManager = auctionLockManager;
        this.antiSnipingService = antiSnipingService;
        this.auctionEventPublisher = auctionEventPublisher;
        this.adminEarlyCloseStates = new ConcurrentHashMap<>();
    }

    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime,
                              String category, String imageSource, int sellerId) throws ValidationException {
        createAuction(
                name,
                desc,
                startPrice,
                startTime,
                endTime,
                category,
                imageSource,
                sellerId,
                true,
                AuctionItem.DEFAULT_EXTENSION_THRESHOLD_SECONDS,
                AuctionItem.DEFAULT_EXTENSION_DURATION_SECONDS,
                AuctionItem.DEFAULT_MAX_EXTENSION_COUNT
        );
    }

    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime,
                              String category, String imageSource, int sellerId, boolean antiSnipingEnabled,
                              int extensionThresholdSeconds, int extensionDurationSeconds, int maxExtensionCount)
            throws ValidationException {
        String normalizedName = normalizeRequiredText(name, "Product name cannot be empty.");
        validateAuctionWindow(startPrice, startTime, endTime, true);
        validateAntiSnipingConfiguration(
                antiSnipingEnabled,
                extensionThresholdSeconds,
                extensionDurationSeconds,
                maxExtensionCount
        );

        AuctionItem item = new AuctionItem(
                0,
                normalizedName,
                desc,
                startPrice,
                startTime,
                endTime,
                category,
                normalizeOptionalText(imageSource),
                sellerId
        );
        item.setOriginalEndTime(endTime);
        item.setExtensionCount(0);
        item.setAntiSnipingEnabled(antiSnipingEnabled);
        item.setExtensionThresholdSeconds(extensionThresholdSeconds);
        item.setExtensionDurationSeconds(extensionDurationSeconds);
        item.setMaxExtensionCount(maxExtensionCount);
        item.setStatus(resolveStatusForWindow(startTime, endTime, System.currentTimeMillis()));
        auctionDAO.saveAuction(item);
    }

    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice,
                              long startTime, long endTime, String category, String imageSource)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        updateAuction(
                auctionId,
                sellerId,
                name,
                desc,
                startPrice,
                startTime,
                endTime,
                category,
                imageSource,
                true,
                AuctionItem.DEFAULT_EXTENSION_THRESHOLD_SECONDS,
                AuctionItem.DEFAULT_EXTENSION_DURATION_SECONDS,
                AuctionItem.DEFAULT_MAX_EXTENSION_COUNT
        );
    }

    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice,
                              long startTime, long endTime, String category, String imageSource,
                              boolean antiSnipingEnabled, int extensionThresholdSeconds,
                              int extensionDurationSeconds, int maxExtensionCount)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = requireAuctionItem(auctionId);
            ensureSellerOwnsAuction(item, sellerId, "edit");
            ensureAuctionCanBeEdited(item, auctionId);

            String normalizedName = normalizeRequiredText(name, "Product name cannot be empty.");
            validateAuctionWindow(startPrice, startTime, endTime, false);
            validateAntiSnipingConfiguration(
                    antiSnipingEnabled,
                    extensionThresholdSeconds,
                    extensionDurationSeconds,
                    maxExtensionCount
            );

            item.setName(normalizedName);
            item.setDescription(desc);
            item.setStartPrice(startPrice);
            item.setCurrentHighestBid(startPrice);
            item.setStartTime(startTime);
            item.setEndTime(endTime);
            item.setOriginalEndTime(endTime);
            item.setExtensionCount(0);
            item.setAntiSnipingEnabled(antiSnipingEnabled);
            item.setExtensionThresholdSeconds(extensionThresholdSeconds);
            item.setExtensionDurationSeconds(extensionDurationSeconds);
            item.setMaxExtensionCount(maxExtensionCount);
            item.setCategory(category);
            item.setImageSource(normalizeOptionalText(imageSource));
            item.setStatus(resolveStatusForWindow(startTime, endTime, System.currentTimeMillis()));
            item.setUpdatedAt(System.currentTimeMillis());
            auctionDAO.updateAuction(item);
        } finally {
            lock.unlock();
        }
    }

    public void deleteAuction(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = requireAuctionItem(auctionId);
            ensureSellerOwnsAuction(item, sellerId, "delete or cancel");

            List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
            if (bids.isEmpty()) {
                auctionDAO.deleteAuction(auctionId);
            } else {
                item.setStatus(AuctionStatus.CANCELED);
                item.setUpdatedAt(System.currentTimeMillis());
                auctionDAO.updateAuction(item);
            }
            adminEarlyCloseStates.remove(auctionId);
        } finally {
            lock.unlock();
        }
    }

    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        return auctionDAO.findAuctionsBySeller(sellerId);
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
        placeBid(new BidRequest(auctionId, bidderId, amount));
    }

    public BidResult placeBid(BidRequest request)
            throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        ReentrantLock lock = getLockForAuction(request.getAuctionId());
        lock.lock();
        try {
            AuctionItem item = requireAuctionForBidding(request.getAuctionId());
            ensureBidderCanBid(item, request.getBidderId());
            validateBidAmount(item, request.getAmount());

            Map<Integer, Double> walletBalances = new HashMap<>();
            ensureSufficientBalance(request.getBidderId(), request.getAmount(), walletBalances);

            long now = System.currentTimeMillis();
            long previousEndTime = item.getEndTime();
            boolean auctionExtended = antiSnipingService.extendAuction(item, now);
            BidTransaction acceptedBid = persistBid(item, request.getBidderId(), request.getAmount(), now, "ACCEPTED");

            AutoBid nextAttacker;
            while ((nextAttacker = selectNextAutoBidder(request.getAuctionId(), item, walletBalances)) != null) {
                double autoBidAmount = item.getCurrentHighestBid() + nextAttacker.getIncrement();
                long autoBidTime = System.currentTimeMillis();
                persistBid(item, nextAttacker.getBidderId(), autoBidAmount, autoBidTime, "AUTO_BID");
            }

            refreshEarlyCloseSnapshot(request.getAuctionId(), item, System.currentTimeMillis());
            BidResult result = buildBidResult(item, acceptedBid, previousEndTime, auctionExtended);
            publishBidEvents(result);
            return result;
        } finally {
            lock.unlock();
        }
    }

    public void closeAuctionManually(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, AuctionClosedException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = requireAuctionItem(auctionId);
            ensureSellerOwnsAuction(item, sellerId, "close");
            if (item.getStatus() == AuctionStatus.FINISHED
                    || item.getStatus() == AuctionStatus.CANCELED
                    || item.getStatus() == AuctionStatus.PAID) {
                throw new AuctionClosedException("The auction has already ended or was cancelled.");
            }

            processAuctionClosing(item.getId(), System.currentTimeMillis());
        } finally {
            lock.unlock();
        }
    }

    public void extendAuctionTime(int auctionId, int sellerId, int additionalMinutes)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = requireAuctionItem(auctionId);
            ensureSellerOwnsAuction(item, sellerId, "extend");

            if (additionalMinutes <= 0) {
                throw new ValidationException("Additional minutes must be greater than 0.");
            }

            long now = System.currentTimeMillis();
            if (item.getStatus() == AuctionStatus.FINISHED
                    || item.getStatus() == AuctionStatus.CANCELED
                    || item.getStatus() == AuctionStatus.PAID
                    || now >= item.getEndTime()) {
                throw new ValidationException("This auction can only be extended while it is OPEN or RUNNING.");
            }

            long additionalMillis = additionalMinutes * 60_000L;
            item.setEndTime(item.getEndTime() + additionalMillis);
            item.setStatus(resolveStatusForWindow(item.getStartTime(), item.getEndTime(), now));
            item.setUpdatedAt(now);
            auctionDAO.updateAuction(item);
            adminEarlyCloseStates.remove(auctionId);
        } finally {
            lock.unlock();
        }
    }

    public void startAdminEarlyCloseCountdown(int auctionId)
            throws ItemNotFoundException, AuctionClosedException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = requireAuctionItem(auctionId);
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
            requireAuctionItem(auctionId);
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
        for (AuctionItem snapshot : auctionDAO.findAllAuctions()) {
            ReentrantLock lock = getLockForAuction(snapshot.getId());
            lock.lock();
            try {
                AuctionItem item = auctionDAO.findAuctionById(snapshot.getId());
                if (item == null) {
                    adminEarlyCloseStates.remove(snapshot.getId());
                    continue;
                }

                AuctionStatus currentStatus = item.getStatus();

                if (currentStatus == AuctionStatus.FINISHED
                        || currentStatus == AuctionStatus.PAID
                        || currentStatus == AuctionStatus.CANCELED) {
                    adminEarlyCloseStates.remove(item.getId());
                    continue;
                }

                if (currentStatus == AuctionStatus.OPEN
                        && now >= item.getStartTime()
                        && now < item.getEndTime()) {
                    item.setStatus(AuctionStatus.RUNNING);
                    item.setUpdatedAt(now);
                    auctionDAO.updateAuction(item);
                    continue;
                }

                if ((currentStatus == AuctionStatus.OPEN || currentStatus == AuctionStatus.RUNNING)
                        && now >= item.getEndTime()) {
                    processAuctionClosing(item.getId(), now);
                }
            } finally {
                lock.unlock();
            }
        }

        tickAdminEarlyCloseCountdowns(now);
    }

    private ReentrantLock getLockForAuction(int auctionId) {
        return auctionLockManager.getLock(auctionId);
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

                if (now - state.lastTickAt < EARLY_CLOSE_TICK_INTERVAL_MS) {
                    continue;
                }

                List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
                long latestBidTimestamp = findLatestBidTimestamp(bids);
                if (bids.size() != state.observedBidCount
                        || Double.compare(item.getCurrentHighestBid(), state.observedHighestBid) != 0
                        || latestBidTimestamp != state.observedLatestBidTimestamp) {
                    state.reset(bids.size(), item.getCurrentHighestBid(), latestBidTimestamp, now);
                    continue;
                }

                state.lastTickAt = now;
                state.remainingCounts--;
                if (state.remainingCounts <= 0) {
                    processAuctionClosing(item.getId(), now);
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

    private AuctionItem requireAuctionItem(int auctionId) throws ItemNotFoundException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) {
            throw new ItemNotFoundException("Auction item not found.");
        }
        return item;
    }

    private AuctionItem requireAuctionForBidding(int auctionId)
            throws ItemNotFoundException, AuctionClosedException {
        AuctionItem item = requireAuctionItem(auctionId);
        long now = System.currentTimeMillis();
        if (item.getStatus() == AuctionStatus.OPEN && now >= item.getStartTime() && now < item.getEndTime()) {
            item.setStatus(AuctionStatus.RUNNING);
            item.setUpdatedAt(now);
            auctionDAO.updateAuction(item);
        }
        if (item.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("The auction is not currently running.");
        }
        if (now < item.getStartTime() || now >= item.getEndTime()) {
            throw new AuctionClosedException("The current time is not valid for bidding.");
        }
        return item;
    }

    private void ensureSellerOwnsAuction(AuctionItem item, int sellerId, String action)
            throws UnauthorizedException {
        if (item.getSellerId() != sellerId) {
            throw new UnauthorizedException("Only the creator can " + action + " this item.");
        }
    }

    private void ensureAuctionCanBeEdited(AuctionItem item, int auctionId) throws ValidationException {
        if (!auctionDAO.findBidsByAuction(auctionId).isEmpty()) {
            throw new ValidationException("This item already has bids and can no longer be edited.");
        }
        if (item.getStatus() != AuctionStatus.OPEN || System.currentTimeMillis() >= item.getStartTime()) {
            throw new ValidationException("This item can only be edited before it starts or while it is in OPEN status.");
        }
    }

    private void validateAuctionWindow(double startPrice, long startTime, long endTime, boolean requireFutureEndTime)
            throws ValidationException {
        if (startPrice <= 0) {
            throw new ValidationException("Starting price must be greater than 0.");
        }
        if (startTime <= 0) {
            throw new ValidationException("Start time is invalid.");
        }
        if (startTime >= endTime) {
            throw new ValidationException("Start time must be earlier than end time.");
        }
        if (endTime <= System.currentTimeMillis()) {
            throw new ValidationException(requireFutureEndTime
                    ? "Cannot create an expired auction."
                    : "Auction end time must be in the future.");
        }
    }

    private void validateAntiSnipingConfiguration(boolean antiSnipingEnabled, int extensionThresholdSeconds,
                                                  int extensionDurationSeconds, int maxExtensionCount)
            throws ValidationException {
        if (extensionThresholdSeconds <= 0) {
            throw new ValidationException("Extension threshold must be greater than 0 seconds.");
        }
        if (extensionDurationSeconds <= 0) {
            throw new ValidationException("Extension duration must be greater than 0 seconds.");
        }
        if (maxExtensionCount <= 0) {
            throw new ValidationException("Maximum extension count must be greater than 0.");
        }
        if (!antiSnipingEnabled) {
            return;
        }
        if (extensionDurationSeconds < extensionThresholdSeconds) {
            throw new ValidationException("Extension duration should be greater than or equal to the threshold window.");
        }
    }

    private String normalizeRequiredText(String value, String errorMessage) throws ValidationException {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new ValidationException(errorMessage);
        }
        return normalized;
    }

    private void validateBidAmount(AuctionItem item, double amount) throws InvalidBidException {
        if (amount <= item.getStartPrice()) {
            throw new InvalidBidException(
                    "The amount must be higher than the starting price (" + item.getStartPrice() + ")."
            );
        }
        if (amount <= item.getCurrentHighestBid()) {
            throw new InvalidBidException(
                    "The amount must be higher than the current price (" + item.getCurrentHighestBid() + ")."
            );
        }
    }

    private void ensureBidderCanBid(AuctionItem item, int bidderId) throws InvalidBidException {
        if (bidderId == item.getSellerId()) {
            throw new InvalidBidException("Sellers cannot place bids on their own auctions.");
        }
    }

    private void ensureSufficientBalance(int bidderId, double amount, Map<Integer, Double> walletBalances)
            throws ItemNotFoundException, InvalidBidException {
        double balance = resolveWalletBalance(bidderId, walletBalances);
        if (balance < amount) {
            throw new InvalidBidException(
                    "Wallet balance (" + AuctionViewFormatter.formatMoney(balance) + ") is not enough for this bid."
            );
        }
    }

    private AutoBid selectNextAutoBidder(int auctionId, AuctionItem item, Map<Integer, Double> walletBalances) {
        List<AutoBid> activeAutoBids = autoBidDAO.findAutoBidsByAuction(auctionId);
        AutoBid nextAttacker = null;
        double currentPrice = item.getCurrentHighestBid();

        for (AutoBid autoBid : activeAutoBids) {
            if (autoBid.getBidderId() == item.getWinnerId()) {
                continue;
            }
            if (autoBid.getBidderId() == item.getSellerId()) {
                continue;
            }

            double requiredBid = currentPrice + autoBid.getIncrement();
            if (requiredBid > autoBid.getMaxPrice()) {
                continue;
            }

            Double balance = tryResolveWalletBalance(autoBid.getBidderId(), walletBalances);
            if (balance == null || balance < requiredBid) {
                continue;
            }

            if (nextAttacker == null
                    || Double.compare(autoBid.getMaxPrice(), nextAttacker.getMaxPrice()) > 0
                    || (Double.compare(autoBid.getMaxPrice(), nextAttacker.getMaxPrice()) == 0
                    && autoBid.getId() < nextAttacker.getId())) {
                nextAttacker = autoBid;
            }
        }

        return nextAttacker;
    }

    private double resolveWalletBalance(int userId, Map<Integer, Double> walletBalances) throws ItemNotFoundException {
        Double cachedBalance = walletBalances.get(userId);
        if (cachedBalance != null) {
            return cachedBalance;
        }

        Wallet wallet = walletService.getWallet(userId);
        double balance = wallet.getBalance();
        walletBalances.put(userId, balance);
        return balance;
    }

    private Double tryResolveWalletBalance(int userId, Map<Integer, Double> walletBalances) {
        try {
            return resolveWalletBalance(userId, walletBalances);
        } catch (ItemNotFoundException ex) {
            return null;
        }
    }

    private BidTransaction persistBid(AuctionItem item, int bidderId, double amount, long bidTime, String status) {
        BidTransaction bid = new BidTransaction(0, item.getId(), bidderId, amount, bidTime, status);
        item.setCurrentHighestBid(amount);
        item.setWinnerId(bidderId);
        item.setUpdatedAt(bidTime);
        auctionDAO.saveBidAndUpdateAuction(bid, item);
        return bid;
    }

    private BidResult buildBidResult(AuctionItem item, BidTransaction acceptedBid, long previousEndTime,
                                     boolean auctionExtended) {
        boolean bidderIsLeading = item.getWinnerId() == acceptedBid.getBidderId();
        String message = bidderIsLeading
                ? "Bid accepted and this bidder is currently leading."
                : "Bid accepted, but a higher automatic bid is currently leading.";
        if (auctionExtended) {
            message += " Auction end time has been extended.";
        }
        return new BidResult(
                true,
                acceptedBid.getId(),
                item.getId(),
                acceptedBid.getBidderId(),
                acceptedBid.getAmount(),
                item.getCurrentHighestBid(),
                item.getWinnerId(),
                previousEndTime,
                item.getEndTime(),
                auctionExtended,
                item.getExtensionCount(),
                item.getStatus(),
                message
        );
    }

    private void publishBidEvents(BidResult result) {
        AuctionUpdateEvent bidAcceptedEvent = new AuctionUpdateEvent(
                AuctionEventType.BID_ACCEPTED,
                result.getAuctionId(),
                result.getBidId(),
                result.getCurrentHighestBid(),
                result.getCurrentWinnerId(),
                result.getPreviousEndTime(),
                result.getAuctionEndTime(),
                result.getExtensionCount()
        );
        auctionEventPublisher.publish(bidAcceptedEvent);

        if (!result.isAuctionExtended()) {
            return;
        }

        AuctionUpdateEvent auctionExtendedEvent = new AuctionUpdateEvent(
                AuctionEventType.AUCTION_EXTENDED,
                result.getAuctionId(),
                result.getBidId(),
                result.getCurrentHighestBid(),
                result.getCurrentWinnerId(),
                result.getPreviousEndTime(),
                result.getAuctionEndTime(),
                result.getExtensionCount()
        );
        auctionEventPublisher.publish(auctionExtendedEvent);
    }

    private static long findLatestBidTimestamp(List<BidTransaction> bids) {
        long latestTimestamp = NO_BID_TIMESTAMP;
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

    private AuctionStatus resolveStatusForWindow(long startTime, long endTime, long now) {
        if (now >= endTime) {
            return AuctionStatus.FINISHED;
        }
        if (now >= startTime) {
            return AuctionStatus.RUNNING;
        }
        return AuctionStatus.OPEN;
    }

    private void processAuctionClosing(int auctionId, long now) {
        if (!auctionDAO.markAuctionFinished(auctionId, now, now)) {
            adminEarlyCloseStates.remove(auctionId);
            return;
        }

        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) {
            adminEarlyCloseStates.remove(auctionId);
            return;
        }

        if (item.getWinnerId() > 0) {
            try {
                walletService.deductFromWallet(item.getWinnerId(), item.getCurrentHighestBid());
                item.setStatus(AuctionStatus.PAID);
            } catch (Exception ex) {
                System.err.println("Auction payment error for auction " + item.getId() + ": " + ex.getMessage());
            }
        }
        item.setEndTime(now);
        item.setUpdatedAt(now);
        auctionDAO.updateAuction(item);
        adminEarlyCloseStates.remove(auctionId);
    }

    private static final class AdminEarlyCloseState {
        private int remainingCounts;
        private int observedBidCount;
        private double observedHighestBid;
        private long observedLatestBidTimestamp;
        private long lastTickAt;

        private static AdminEarlyCloseState from(AuctionItem item, List<BidTransaction> bids, long now) {
            AdminEarlyCloseState state = new AdminEarlyCloseState();
            state.reset(bids.size(), item.getCurrentHighestBid(), findLatestBidTimestamp(bids), now);
            return state;
        }

        private void reset(int bidCount, double highestBid, long latestBidTimestamp, long now) {
            this.remainingCounts = ADMIN_EARLY_CLOSE_COUNTS;
            this.observedBidCount = bidCount;
            this.observedHighestBid = highestBid;
            this.observedLatestBidTimestamp = latestBidTimestamp;
            this.lastTickAt = now;
        }
    }
}
