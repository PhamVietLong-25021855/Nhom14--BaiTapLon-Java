package userauth.service;

import userauth.common.AuctionRules;
import userauth.dao.AuctionDAO;
import userauth.dao.AutoBidDAO;
import userauth.event.AuctionEvent;
import userauth.event.AuctionEventBus;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.AutoBid;
import userauth.model.BidTransaction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService implements userauth.api.AuctionApi {
    private static final long REQUEST_STATUS_REFRESH_INTERVAL_MS = 1000L;

    private final AuctionDAO auctionDAO;
    private final AutoBidDAO autoBidDAO;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks;
    private final ConcurrentHashMap<Integer, AdminEarlyCloseState> adminEarlyCloseStates;
    private final AuctionSettlementHandlerFactory settlementHandlerFactory;
    private final AuctionEventBus eventBus;
    private final ReentrantLock statusRefreshLock;
    private volatile long lastStatusRefreshAt;

    public AuctionService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO) {
        this(auctionDAO, autoBidDAO, null, null);
    }

    public AuctionService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO, WalletService walletService, NotificationService notificationService) {
        this.auctionDAO = auctionDAO;
        this.autoBidDAO = autoBidDAO;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.auctionLocks = new ConcurrentHashMap<>();
        this.adminEarlyCloseStates = new ConcurrentHashMap<>();
        this.settlementHandlerFactory = new AuctionSettlementHandlerFactory();
        this.eventBus = AuctionEventBus.getInstance();
        this.statusRefreshLock = new ReentrantLock();
    }

    private ReentrantLock getLockForAuction(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, ignored -> new ReentrantLock());
    }

    @Override
    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime,
                             String category, String imageSource, byte[] imageData, int sellerId) throws ValidationException {
        if (name == null || name.trim().isEmpty()) throw new ValidationException("Product name cannot be empty.");
        if (startPrice <= 0) throw new ValidationException("Starting price must be greater than 0.");
        if (startTime >= endTime) throw new ValidationException("Start time must be earlier than end time.");
        if (endTime <= System.currentTimeMillis()) throw new ValidationException("Cannot create an expired auction.");
        validateImage(imageData);
        AuctionItem item = new AuctionItem(0, name, desc, startPrice, startTime, endTime, category, normalizeOptionalText(imageSource), imageData, sellerId);
        auctionDAO.saveAuction(item);
        notificationService.createNotification(0, "New auction", "New auction has been created: " + name);
    }

    @Override
    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice,
                             long startTime, long endTime, String category, String imageSource, byte[] imageData)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) throw new ItemNotFoundException("Auction item not found.");
        if (item.getSellerId() != sellerId) throw new UnauthorizedException("Only the creator can edit this item.");
        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        if (!bids.isEmpty()) throw new ValidationException("This item already has bids and can no longer be edited.");
        if (item.getStatus() == AuctionStatus.RUNNING || item.getStatus() == AuctionStatus.FINISHED)
            throw new ValidationException("This item can only be edited before it starts or while it is in OPEN status.");
        if (name == null || name.trim().isEmpty()) throw new ValidationException("Product name cannot be empty.");
        if (startPrice <= 0) throw new ValidationException("Starting price must be greater than 0.");
        if (startTime >= endTime) throw new ValidationException("Start time must be earlier than end time.");
        validateImage(imageData);
        item.setName(name); item.setDescription(desc); item.setStartPrice(startPrice);
        item.setCurrentHighestBid(startPrice); item.setStartTime(startTime); item.setEndTime(endTime);
        item.setCategory(category); item.setImageSource(normalizeOptionalText(imageSource));
        item.setImageData(imageData); item.setAntiSnipingExtensionCount(0);
        item.setUpdatedAt(System.currentTimeMillis());
        auctionDAO.updateAuction(item);
    }

    @Override
    public void deleteAuction(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException {
        boolean shouldReconcileReservedBalances = false;
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) throw new ItemNotFoundException("Auction item not found.");
            if (item.getSellerId() != sellerId) throw new UnauthorizedException("Only the creator can delete or cancel this item.");
            shouldReconcileReservedBalances = deleteOrCancelAuctionLocked(item, "Auction was cancelled by the seller.");
        } finally {
            lock.unlock();
        }
        if (shouldReconcileReservedBalances) {
            reconcileReservedBalances();
        }
    }

    @Override
    public void deleteAuctionAsAdmin(int auctionId) throws ItemNotFoundException {
        boolean shouldReconcileReservedBalances = false;
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) throw new ItemNotFoundException("Auction item not found.");
            shouldReconcileReservedBalances = deleteOrCancelAuctionLocked(item, "Auction was deleted by an admin.");
        } finally {
            lock.unlock();
        }
        if (shouldReconcileReservedBalances) {
            reconcileReservedBalances();
        }
    }

    private boolean deleteOrCancelAuctionLocked(AuctionItem item, String eventSummary) {
        int auctionId = item.getId();
        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        if (bids.isEmpty()) {
            auctionDAO.deleteAuction(auctionId);
            adminEarlyCloseStates.remove(auctionId);
            return false;
        }

        int refundedWinnerId = item.getWinnerId();
        try {
            releaseOrRefundWinnerFunds(item);
        } catch (ValidationException ex) {
            System.err.println("Unable to release wallet funds while cancelling auction " + auctionId + ": " + ex.getMessage());
        }

        long now = System.currentTimeMillis();
        item.setStatus(AuctionStatus.CANCELED);
        item.setWinnerId(-1);
        item.setUpdatedAt(now);
        auctionDAO.updateAuctionState(item);
        adminEarlyCloseStates.remove(auctionId);
        eventBus.publish(AuctionEvent.statusChanged(item, now, eventSummary));
        notifyAuctionCancellation(auctionId, refundedWinnerId);
        return true;
    }

    @Override
    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        refreshAuctionStatusesIfDue();
        return auctionDAO.findAuctionsBySeller(sellerId);
    }

    @Override
    public List<AuctionItem> getAllAuctions() {
        refreshAuctionStatusesIfDue();
        return auctionDAO.findAllAuctions();
    }

    @Override
    public List<AuctionItem> getAllAuctionSummaries() {
        refreshAuctionStatusesIfDue();
        return auctionDAO.findAllAuctionSummaries();
    }

    @Override
    public AuctionItem getAuctionById(int auctionId) throws ItemNotFoundException {
        refreshSingleAuctionStatusIfDue(auctionId);
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) throw new ItemNotFoundException("Auction item not found.");
        return item;
    }

    @Override
    public List<BidTransaction> getBidsForAuction(int auctionId) {
        return auctionDAO.findBidsByAuction(auctionId);
    }

    @Override
    public List<BidTransaction> getAllBids() {
        return auctionDAO.findAllBids();
    }

    @Override
    public int countAllBids() {
        return auctionDAO.countAllBids();
    }

    @Override
    public Map<Integer, Integer> getBidCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (BidTransaction bid : auctionDAO.findAllBids()) {
            counts.merge(bid.getAuctionId(), 1, Integer::sum);
        }
        return counts;
    }

     @Override
     public void placeBid(int auctionId, int bidderId, double amount)
             throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
         ReentrantLock lock = getLockForAuction(auctionId);
         lock.lock();
         try {
             AuctionItem item = auctionDAO.findAuctionById(auctionId);
             if (item == null) throw new ItemNotFoundException("Auction item not found.");
             refreshAuctionStatusLocked(item, System.currentTimeMillis());
             if (item.getStatus() != AuctionStatus.RUNNING) throw new AuctionClosedException("The auction is not currently running.");
             long now = System.currentTimeMillis();
             if (now < item.getStartTime() || now > item.getEndTime())
                 throw new AuctionClosedException("The current time is not valid for bidding.");
             if (item.getWinnerId() == bidderId)
                 throw new InvalidBidException("You are already the leading bidder for this auction.");
             if (amount <= item.getStartPrice())
                 throw new InvalidBidException("The amount must be higher than the starting price (" + item.getStartPrice() + ").");
             if (amount <= item.getCurrentHighestBid())
                 throw new InvalidBidException("The amount must be higher than the current price (" + item.getCurrentHighestBid() + ").");
             int originalWinnerId = item.getWinnerId();
             double originalWinningAmount = originalWinnerId > 0 ? item.getCurrentHighestBid() : 0.0;

             // Check funds availability BEFORE processing the bid
             try {
                 ensureBidFundsAvailable(bidderId, amount, originalWinnerId, originalWinningAmount);
             } catch (ValidationException ex) {
                 throw new InvalidBidException(ex.getMessage());
             }

             List<BidTransaction> pendingBids = new ArrayList<>();
             long eventTime = now;
             pendingBids.add(new BidTransaction(0, auctionId, bidderId, amount, eventTime, "ACCEPTED"));
             item.setCurrentHighestBid(amount);
             item.setWinnerId(bidderId);
             item.setUpdatedAt(eventTime);

             try {
                 eventTime = applyAutoBids(item, eventTime, originalWinnerId, originalWinningAmount, pendingBids);
                 boolean antiSnipingExtended = applyAntiSniping(item, now, eventTime);

                 // Apply fund reservation with the final winning state after all autobids
                 applyReservationTransition(originalWinnerId, originalWinningAmount, item.getWinnerId(), item.getCurrentHighestBid());

                 persistBidSequence(pendingBids);
                 auctionDAO.updateAuctionState(item);
                 refreshEarlyCloseSnapshot(auctionId, item, now);
                 eventBus.publish(AuctionEvent.bidActivity(item, item.getUpdatedAt()));
                 if (antiSnipingExtended) eventBus.publish(AuctionEvent.antiSnipingExtended(item, item.getUpdatedAt()));
             } catch (ValidationException ex) {
                 throw new InvalidBidException(ex.getMessage());
             }
         } finally {
             lock.unlock();
         }
     }

    public void triggerAutoBids(int auctionId) {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null || item.getStatus() != AuctionStatus.RUNNING) return;
            long now = System.currentTimeMillis();
            int originalWinnerId = item.getWinnerId();
            double originalWinningAmount = originalWinnerId > 0 ? item.getCurrentHighestBid() : 0.0;
            applyAutoBidsAndPublish(item, now, originalWinnerId, originalWinningAmount);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeAuctionManually(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, AuctionClosedException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) throw new ItemNotFoundException("Item not found.");
        if (item.getSellerId() != sellerId) throw new UnauthorizedException("You do not have permission to close this auction.");
        if (item.getStatus() == AuctionStatus.FINISHED || item.getStatus() == AuctionStatus.CANCELED || item.getStatus() == AuctionStatus.PAID)
            throw new AuctionClosedException("The auction has already ended or was cancelled.");
        item.setStatus(AuctionStatus.FINISHED);
        long now = System.currentTimeMillis();
        finishAuction(item, now, "manual close");
        auctionDAO.updateAuctionState(item);
        adminEarlyCloseStates.remove(auctionId);
        eventBus.publish(AuctionEvent.statusChanged(item, item.getUpdatedAt(), "Auction closed manually."));
        try {
            notificationService.createNotification(0, "Auction closed", "Auction " + auctionId + " has been closed by" + sellerId);
            notificationService.createNotification(item.getWinnerId(), "You won", "You have won " + auctionId + " auction");
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startAdminEarlyCloseCountdown(int auctionId)
            throws ItemNotFoundException, AuctionClosedException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) throw new ItemNotFoundException("Auction not found.");
            if (item.getStatus() != AuctionStatus.RUNNING)
                throw new AuctionClosedException("Early-close countdown is only available while the auction is RUNNING.");
            if (adminEarlyCloseStates.containsKey(auctionId))
                throw new ValidationException("This auction is already in an early-close countdown process.");
            List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
            adminEarlyCloseStates.put(auctionId, AdminEarlyCloseState.from(item, bids, System.currentTimeMillis()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cancelAdminEarlyCloseCountdown(int auctionId) throws ItemNotFoundException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) throw new ItemNotFoundException("Auction not found.");
            if (adminEarlyCloseStates.remove(auctionId) == null)
                throw new ValidationException("This auction has not activated the early-close countdown.");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<Integer, Integer> getAdminEarlyCloseCountdowns() {
        Map<Integer, Integer> countdowns = new HashMap<>();
        adminEarlyCloseStates.forEach((auctionId, state) -> countdowns.put(auctionId, state.remainingCounts));
        return countdowns;
    }

    @Override
    public void markAuctionAsPaid(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        settleFinishedAuction(auctionId, sellerId, AuctionStatus.PAID);
    }

    @Override
    public void cancelFinishedAuction(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        settleFinishedAuction(auctionId, sellerId, AuctionStatus.CANCELED);
    }

    @Override
    public void refreshAuctionStatuses() {
        statusRefreshLock.lock();
        try {
            refreshAuctionStatusesLocked(System.currentTimeMillis());
            lastStatusRefreshAt = System.currentTimeMillis();
        } finally {
            statusRefreshLock.unlock();
        }
    }

    private void refreshAuctionStatusesIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastStatusRefreshAt < REQUEST_STATUS_REFRESH_INTERVAL_MS) {
            return;
        }
        if (!statusRefreshLock.tryLock()) {
            return;
        }
        try {
            now = System.currentTimeMillis();
            if (now - lastStatusRefreshAt < REQUEST_STATUS_REFRESH_INTERVAL_MS) {
                return;
            }
            refreshAuctionStatusesLocked(now);
            lastStatusRefreshAt = System.currentTimeMillis();
        } finally {
            statusRefreshLock.unlock();
        }
    }

    private void refreshSingleAuctionStatusIfDue(int auctionId) {
        long now = System.currentTimeMillis();
        if (now - lastStatusRefreshAt < REQUEST_STATUS_REFRESH_INTERVAL_MS) {
            return;
        }
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item != null) {
                refreshAuctionStatusLocked(item, now);
            }
        } finally {
            lock.unlock();
        }
    }

    private void refreshAuctionStatusesLocked(long now) {
        for (AuctionItem snapshot : auctionDAO.findStatusRefreshCandidates(now)) {
            ReentrantLock lock = getLockForAuction(snapshot.getId());
            lock.lock();
            try {
                refreshAuctionStatusLocked(snapshot, now);
            } finally {
                lock.unlock();
            }
        }
        tickAdminEarlyCloseCountdowns(now);
    }

    private void refreshAuctionStatusLocked(AuctionItem item, long now) {
        AuctionStatus currentStatus = item.getStatus();
        if (currentStatus == AuctionStatus.FINISHED || currentStatus == AuctionStatus.PAID || currentStatus == AuctionStatus.CANCELED) {
            adminEarlyCloseStates.remove(item.getId());
            return;
        }
        if (currentStatus == AuctionStatus.OPEN && now >= item.getStartTime() && now < item.getEndTime()) {
            item.setStatus(AuctionStatus.RUNNING);
            item.setUpdatedAt(now);
            int originalWinnerId = item.getWinnerId();
            double originalWinningAmount = originalWinnerId > 0 ? item.getCurrentHighestBid() : 0.0;
            List<BidTransaction> pendingBids = new ArrayList<>();
            long newEventTime = now;
            boolean autoBidPlaced = false;
            boolean antiSnipingExtended = false;
            try {
                newEventTime = applyAutoBids(item, now, originalWinnerId, originalWinningAmount, pendingBids);
                autoBidPlaced = newEventTime > now;
                antiSnipingExtended = autoBidPlaced && applyAntiSniping(item, now, newEventTime);
                if (autoBidPlaced) {
                    applyReservationTransition(originalWinnerId, originalWinningAmount, item.getWinnerId(), item.getCurrentHighestBid());
                    persistBidSequence(pendingBids);
                }
            } catch (ItemNotFoundException | ValidationException ex) {
                autoBidPlaced = false;
            }
            auctionDAO.updateAuctionState(item);
            eventBus.publish(AuctionEvent.statusChanged(item, now, "Auction is now running."));
            if (autoBidPlaced) {
                refreshEarlyCloseSnapshot(item.getId(), item, item.getUpdatedAt());
                eventBus.publish(AuctionEvent.bidActivity(item, item.getUpdatedAt()));
            }
            if (antiSnipingExtended) eventBus.publish(AuctionEvent.antiSnipingExtended(item, item.getUpdatedAt()));
            return;
        }
        if (currentStatus == AuctionStatus.RUNNING && now < item.getEndTime()) {
            int originalWinnerId = item.getWinnerId();
            double originalWinningAmount = originalWinnerId > 0 ? item.getCurrentHighestBid() : 0.0;
            applyAutoBidsAndPublish(item, now, originalWinnerId, originalWinningAmount);
            return;
        }
        if ((currentStatus == AuctionStatus.OPEN || currentStatus == AuctionStatus.RUNNING) && now >= item.getEndTime()) {
            finishAuction(item, now, "scheduled close");
            auctionDAO.updateAuctionState(item);
            adminEarlyCloseStates.remove(item.getId());
            eventBus.publish(AuctionEvent.statusChanged(item, now, "Auction has finished."));
            if (notificationService != null) {
                try {
                    notificationService.createNotification(0, "Auction closed", "Auction " + item.getId() + " has ran out of time");
                    if (item.getWinnerId() > 0) {
                        notificationService.createNotification(item.getWinnerId(), "You won", "You have won " + item.getId() + "auction");
                    }
                } catch (ValidationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void reconcileReservedBalances() {
        if (walletService == null) {
            return;
        }

        List<Integer> auctionIds = auctionDAO.findAllAuctionIds();
        List<ReentrantLock> locks = new ArrayList<>();
        for (int auctionId : auctionIds) {
            ReentrantLock lock = getLockForAuction(auctionId);
            lock.lock();
            locks.add(lock);
        }

        try {
            captureFinishedAuctionPayments();
            Map<Integer, Long> expectedReservedByUser = new HashMap<>();
            for (BidTransaction bid : auctionDAO.findAllBids()) {
                if (bid.getBidderId() > 0) {
                    expectedReservedByUser.putIfAbsent(bid.getBidderId(), 0L);
                }
            }
            for (AuctionItem item : auctionDAO.findAuctionsHoldingReservedFunds()) {
                if (holdsReservedFunds(item)) {
                    expectedReservedByUser.merge(
                            item.getWinnerId(),
                            (long) item.getCurrentHighestBid(),
                            Long::sum
                    );
                }
            }
            for (Map.Entry<Integer, Long> entry : expectedReservedByUser.entrySet()) {
                try {
                    walletService.reconcileReservedBalance(entry.getKey(), entry.getValue());
                } catch (ValidationException | RuntimeException ex) {
                    System.err.println("Unable to reconcile reserved wallet balance for user " +
                            entry.getKey() + ": " + ex.getMessage());
                }
            }
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                locks.get(i).unlock();
            }
        }
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
                if (now - state.lastTickAt < 1000) continue;
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
                    finishAuction(item, now, "admin early-close countdown");
                    auctionDAO.updateAuctionState(item);
                    adminEarlyCloseStates.remove(auctionId);
                    eventBus.publish(AuctionEvent.statusChanged(item, now, "Auction finished after the admin early-close countdown."));
                    try {
                        notificationService.createNotification(0, "Auction closed", "Auction " + auctionId + " has been closed by admin");
                        notificationService.createNotification(item.getWinnerId(), "You won", "You have won " + auctionId + "auction");
                    } catch (ValidationException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void refreshEarlyCloseSnapshot(int auctionId, AuctionItem item, long now) {
        AdminEarlyCloseState state = adminEarlyCloseStates.get(auctionId);
        if (state == null) return;
        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        state.reset(bids.size(), item.getCurrentHighestBid(), findLatestBidTimestamp(bids), now);
    }

    private long findLatestBidTimestamp(List<BidTransaction> bids) {
        long latest = -1;
        for (BidTransaction bid : bids) {
            if (bid.getTimestamp() > latest) latest = bid.getTimestamp();
        }
        return latest;
    }

    private long applyAutoBids(AuctionItem item, long eventTime, int originalWinnerId,
                               double originalWinningAmount, List<BidTransaction> pendingBids)
            throws ValidationException, ItemNotFoundException {
        List<AutoBid> autoBids = autoBidDAO.findAutoBidsByAuction(item.getId());
        if (autoBids.isEmpty()) return eventTime;
        long currentEventTime = eventTime;
        while (true) {
            AutoBid nextBidder = selectNextAutoBidder(
                    autoBids,
                    item.getWinnerId(),
                    item.getCurrentHighestBid(),
                    originalWinnerId,
                    originalWinningAmount
            );
            if (nextBidder == null) return currentEventTime;
            double nextAmount = Math.min(item.getCurrentHighestBid() + nextBidder.getIncrement(), nextBidder.getMaxPrice());
            if (nextAmount <= item.getCurrentHighestBid()) {
                //notificationService.createNotification(nextBidder.getBidderId(),"One of your Autobid had stopped", "Your " + nextBidder.getId() + " autobid had stopped");
                return currentEventTime;
            };
            ensureBidFundsAvailable(nextBidder.getBidderId(), nextAmount, originalWinnerId, originalWinningAmount);
            currentEventTime++;
            pendingBids.add(new BidTransaction(0, item.getId(), nextBidder.getBidderId(), nextAmount, currentEventTime, "ACCEPTED"));
            item.setCurrentHighestBid(nextAmount);
            item.setWinnerId(nextBidder.getBidderId());
            item.setUpdatedAt(currentEventTime);
        }
    }

    private AutoBid selectNextAutoBidder(List<AutoBid> autoBids, int currentWinnerId, double currentHighestBid,
                                         int originalWinnerId, double originalWinningAmount) {
        return autoBids.stream()
                .filter(autoBid -> autoBid.getBidderId() != currentWinnerId)
                .filter(autoBid -> autoBid.getMaxPrice() > currentHighestBid)
                .filter(autoBid -> canCoverAutoBid(
                        autoBid.getBidderId(),
                        Math.min(currentHighestBid + autoBid.getIncrement(), autoBid.getMaxPrice()),
                        originalWinnerId,
                        originalWinningAmount
                ))
                .sorted(Comparator.comparingDouble(AutoBid::getMaxPrice).reversed()
                        .thenComparingLong(AutoBid::getCreatedAt)
                        .thenComparingInt(AutoBid::getId))
                .findFirst().orElse(null);
    }

    private boolean applyAutoBidsAndPublish(AuctionItem item, long now, int originalWinnerId, double originalWinningAmount) {
        List<BidTransaction> pendingBids = new ArrayList<>();
        long newEventTime;
        try {
            newEventTime = applyAutoBids(item, now, originalWinnerId, originalWinningAmount, pendingBids);
        } catch (ValidationException | ItemNotFoundException ex) {
            return false;
        }
        if (newEventTime <= now) return false;
        boolean antiSnipingExtended = applyAntiSniping(item, now, newEventTime);
        try {
            applyReservationTransition(originalWinnerId, originalWinningAmount, item.getWinnerId(), item.getCurrentHighestBid());
        } catch (ItemNotFoundException | ValidationException ex) {
            return false;
        }
        persistBidSequence(pendingBids);
        auctionDAO.updateAuctionState(item);
        refreshEarlyCloseSnapshot(item.getId(), item, item.getUpdatedAt());
        eventBus.publish(AuctionEvent.bidActivity(item, item.getUpdatedAt()));
        if (antiSnipingExtended) eventBus.publish(AuctionEvent.antiSnipingExtended(item, item.getUpdatedAt()));
        return true;
    }

    private boolean applyAntiSniping(AuctionItem item, long now, long eventTime) {
        if (item.getStatus() != AuctionStatus.RUNNING) return false;
        long remaining = item.getEndTime() - now;
        if (remaining > AuctionRules.ANTI_SNIPING_WINDOW_MS) return false;
        if (item.getAntiSnipingExtensionCount() >= AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS) return false;
        long extendedEndTime = Math.max(item.getEndTime(), now + AuctionRules.ANTI_SNIPING_WINDOW_MS);
        if (extendedEndTime == item.getEndTime()) return false;
        long nextEventTime = Math.max(eventTime + 1, now);
        item.setEndTime(extendedEndTime);
        item.setAntiSnipingExtensionCount(item.getAntiSnipingExtensionCount() + 1);
        item.setUpdatedAt(nextEventTime);
        return true;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateImage(byte[] imageData) throws ValidationException {
        if (imageData != null && imageData.length > AuctionRules.MAX_IMAGE_BYTES)
            throw new ValidationException("Image file is too large. Maximum supported size is 5 MB.");
    }

    private void notifyAuctionCancellation(int auctionId, int refundedWinnerId) {
        if (notificationService == null || refundedWinnerId <= 0) {
            return;
        }
        try {
            notificationService.createNotification(
                    refundedWinnerId,
                    "Auction cancelled",
                    "Auction " + auctionId + " was cancelled. Your reserved balance has been released."
            );
        } catch (ValidationException ex) {
            System.err.println("Unable to notify bidder " + refundedWinnerId + " about auction " + auctionId + " cancellation: " + ex.getMessage());
        }
    }

    private void settleFinishedAuction(int auctionId, int sellerId, AuctionStatus targetStatus)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        boolean shouldReconcileReservedBalances = false;
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) throw new ItemNotFoundException("Auction item not found.");
            if (item.getSellerId() != sellerId) throw new UnauthorizedException("Only the creator can settle this auction.");
            if (targetStatus == AuctionStatus.PAID && item.getStatus() == AuctionStatus.PAID)
                return;
            if (targetStatus == AuctionStatus.PAID && item.getStatus() != AuctionStatus.FINISHED)
                throw new ValidationException("Only finished auctions can move to PAID.");
            if (targetStatus == AuctionStatus.CANCELED && item.getStatus() != AuctionStatus.FINISHED && item.getStatus() != AuctionStatus.PAID)
                throw new ValidationException("Only finished or paid auctions can be cancelled.");
            AuctionSettlementHandler settlementHandler = settlementHandlerFactory.create(targetStatus);
            settlementHandler.validate(item);
            long now = System.currentTimeMillis();
            if (walletService != null && targetStatus == AuctionStatus.PAID && item.getWinnerId() > 0 && item.getCurrentHighestBid() > 0) {
                walletService.captureReservedFunds(item.getWinnerId(), (long) item.getCurrentHighestBid());
                shouldReconcileReservedBalances = true;
            } else if (targetStatus == AuctionStatus.CANCELED) {
                try {
                    releaseOrRefundWinnerFunds(item);
                } catch (ValidationException ex) {
                    System.err.println("Unable to release wallet funds while settling auction " + auctionId + ": " + ex.getMessage());
                }
                shouldReconcileReservedBalances = true;
            }
            settlementHandler.apply(item, now);
            auctionDAO.updateAuctionState(item);
            adminEarlyCloseStates.remove(auctionId);
            eventBus.publish(AuctionEvent.settled(item, now, settlementHandler.summary(item)));
        } finally {
            lock.unlock();
        }
        if (shouldReconcileReservedBalances) {
            reconcileReservedBalances();
        }
    }

    private void ensureBidFundsAvailable(int bidderId, double targetBidAmount, int originalWinnerId,
                                         double originalWinningAmount)
            throws ItemNotFoundException, ValidationException {
        if (walletService == null) {
            return;
        }
        walletService.ensureSufficientAvailableBalanceForBid(
                bidderId,
                (long) targetBidAmount,
                (long) reservationCreditForUser(bidderId, originalWinnerId, originalWinningAmount)
        );
    }

    private boolean canCoverAutoBid(int bidderId, double targetBidAmount, int originalWinnerId, double originalWinningAmount) {
        try {
            ensureBidFundsAvailable(bidderId, targetBidAmount, originalWinnerId, originalWinningAmount);
            return true;
        } catch (ItemNotFoundException | ValidationException ex) {
            return false;
        }
    }

    private double reservationCreditForUser(int bidderId, int originalWinnerId, double originalWinningAmount) {
        return bidderId == originalWinnerId ? Math.max(originalWinningAmount, 0.0) : 0.0;
    }

    private void applyReservationTransition(int originalWinnerId, double originalWinningAmount,
                                            int updatedWinnerId, double updatedWinningAmount)
            throws ItemNotFoundException, ValidationException {
        if (walletService == null) {
            return;
        }
        walletService.applyReservationTransition(
                originalWinnerId,
                (long) Math.max(originalWinningAmount, 0.0),
                updatedWinnerId,
                updatedWinnerId > 0 ? (long) Math.max(updatedWinningAmount, 0.0) : 0
        );
    }

    private void persistBidSequence(List<BidTransaction> pendingBids) {
        for (BidTransaction pendingBid : pendingBids) {
            auctionDAO.saveBid(pendingBid);
        }
    }

    private void releaseOrRefundWinnerFunds(AuctionItem item) throws ValidationException {
        if (walletService == null || item.getWinnerId() <= 0 || item.getCurrentHighestBid() <= 0) {
            return;
        }
        try {
            if (item.getStatus() == AuctionStatus.PAID) {
                walletService.refundCapturedFunds(item.getWinnerId(), (long) item.getCurrentHighestBid());
            } else {
                walletService.releaseReservedFunds(item.getWinnerId(), (long) item.getCurrentHighestBid());
            }
        } catch (ItemNotFoundException ex) {
            throw new ValidationException(ex.getMessage());
        }
    }

    private void finishAuction(AuctionItem item, long now, String context) {
        item.setStatus(captureWinnerPayment(item, context) ? AuctionStatus.PAID : AuctionStatus.FINISHED);
        item.setEndTime(now);
        item.setUpdatedAt(now);
    }

    private void captureFinishedAuctionPayments() {
        for (AuctionItem item : auctionDAO.findFinishedAuctions()) {
            ReentrantLock lock = getLockForAuction(item.getId());
            lock.lock();
            try {
                AuctionItem lockedItem = auctionDAO.findAuctionById(item.getId());
                if (lockedItem == null || lockedItem.getStatus() != AuctionStatus.FINISHED) {
                    continue;
                }
                if (captureWinnerPayment(lockedItem, "reserved balance reconciliation")) {
                    long now = System.currentTimeMillis();
                    lockedItem.setStatus(AuctionStatus.PAID);
                    lockedItem.setUpdatedAt(now);
                    auctionDAO.updateAuctionState(lockedItem);
                    eventBus.publish(AuctionEvent.settled(
                            lockedItem,
                            now,
                            "Auction payment was captured automatically."
                    ));
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private boolean captureWinnerPayment(AuctionItem item, String context) {
        if (walletService == null || item == null || item.getWinnerId() <= 0 || item.getCurrentHighestBid() <= 0) {
            return false;
        }
        try {
            walletService.captureReservedFunds(item.getWinnerId(), (long) item.getCurrentHighestBid());
            return true;
        } catch (ItemNotFoundException | ValidationException ex) {
            System.err.println("Unable to capture wallet funds while finishing auction " +
                    item.getId() + " (" + context + "): " + ex.getMessage());
            return false;
        }
    }

    private boolean holdsReservedFunds(AuctionItem item) {
        if (item == null || item.getWinnerId() <= 0 || item.getCurrentHighestBid() <= 0) {
            return false;
        }
        return item.getStatus() == AuctionStatus.RUNNING;
    }

    private static final class AdminEarlyCloseState {
        int remainingCounts;
        int observedBidCount;
        double observedHighestBid;
        long observedLatestBidTimestamp;
        long lastTickAt;

        private static AdminEarlyCloseState from(AuctionItem item, List<BidTransaction> bids, long now) {
            AdminEarlyCloseState state = new AdminEarlyCloseState();
            state.reset(bids.size(), item.getCurrentHighestBid(), latestTimestamp(bids), now);
            return state;
        }

        private void reset(int bidCount, double highestBid, long latestBidTimestamp, long now) {
            this.remainingCounts = AuctionRules.ADMIN_EARLY_CLOSE_COUNTS;
            this.observedBidCount = bidCount;
            this.observedHighestBid = highestBid;
            this.observedLatestBidTimestamp = latestBidTimestamp;
            this.lastTickAt = now;
        }

        private static long latestTimestamp(List<BidTransaction> bids) {
            long latest = -1;
            for (BidTransaction bid : bids) {
                if (bid.getTimestamp() > latest) latest = bid.getTimestamp();
            }
            return latest;
        }
    }
}
