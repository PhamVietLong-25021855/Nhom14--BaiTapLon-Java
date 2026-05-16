package uet.auctionsystem.service;

import uet.auctionsystem.dao.AuctionDAO;
import uet.auctionsystem.event.AuctionEvent;
import uet.auctionsystem.event.AuctionEventBus;
import uet.auctionsystem.exception.AuctionClosedException;
import uet.auctionsystem.exception.InvalidBidException;
import uet.auctionsystem.exception.ItemNotFoundException;
import uet.auctionsystem.exception.UnauthorizedException;
import uet.auctionsystem.exception.ValidationException;
import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.AuctionStatus;
import uet.auctionsystem.model.BidTransaction;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

// Ghi chu file: File service; chua nghiep vu chinh va phoi hop giua controller, DAO va event.
// Khai bao lop AuctionService; chua xu ly nghiep vu va cac quy tac chinh cua he thong.
public class AuctionService {
    // Thuoc tinh: giu tham chieu den AuctionDAO de phoi hop xu ly.
    private final AuctionDAO auctionDAO;
    // Váº«n giá»¯ wallet service cÅ© Ä‘á»ƒ reserve/capture/refund khÃ´ng bá»‹ máº¥t behavior.
    // Thuoc tinh: giu tham chieu den WalletService de phoi hop xu ly.
    private final WalletService walletService;
    // Lock theo auction Ä‘á»ƒ xá»­ lÃ½ bid vÃ  settlement an toÃ n hÆ¡n khi cÃ³ cáº¡nh tranh.
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks;
    // Tráº¡ng thÃ¡i Ä‘áº¿m ngÆ°á»£c Ä‘Ã³ng sá»›m cá»§a admin.
    // KÃªnh phÃ¡t sá»± kiá»‡n cho cÃ¡c dashboard Ä‘ang má»Ÿ.
    // Thuoc tinh: giu tham chieu den AuctionEventBus de phoi hop xu ly.
    private final AuctionEventBus eventBus;
    // Ham tao: khoi tao doi tuong AuctionService voi cac phu thuoc can thiet.
    public AuctionService(AuctionDAO auctionDAO, WalletService walletService) {
        this.auctionDAO = auctionDAO;
        this.walletService = walletService;
        this.auctionLocks = new ConcurrentHashMap<>();
        this.eventBus = AuctionEventBus.getInstance();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get lock for auction.
    private ReentrantLock getLockForAuction(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, ignored -> new ReentrantLock());
    }

    // Giá»¯ overload cÅ© Ä‘á»ƒ khÃ´ng lÃ m gÃ£y nÆ¡i gá»i chÆ°a truyá»n imageData.
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create auction.
    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, int sellerId)
            throws ValidationException {
        createAuction(name, desc, startPrice, startTime, endTime, category, imageSource, null, sellerId);
    }

    // ÄÆ°á»ng táº¡o má»›i Ä‘áº§y Ä‘á»§ cho schema seller/database má»›i.
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create auction.
    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, byte[] imageData, int sellerId)
            throws ValidationException {
        validateAuctionDraft(name, startPrice, startTime, endTime);
        validateImage(imageData);

        AuctionItem item = new AuctionItem(0, name, desc, startPrice, startTime, endTime, category, normalizeOptionalText(imageSource), imageData, sellerId);
        auctionDAO.saveAuction(item);
    }

    // Giá»¯ overload cÅ© cho luá»“ng update khÃ´ng cÃ³ áº£nh binary.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction.
    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        updateAuction(auctionId, sellerId, name, desc, startPrice, startTime, endTime, category, imageSource, null);
    }

    // Update Ä‘áº§y Ä‘á»§: reset anti-sniping vÃ  cáº­p nháº­t láº¡i bytes áº£nh náº¿u seller thay Ä‘á»•i.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction.
    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, byte[] imageData)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        AuctionItem item = requireOwnedAuction(auctionId, sellerId);

        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        if (!bids.isEmpty()) {
            throw new ValidationException("This item already has bids and can no longer be edited.");
        }
        if (item.getStatus() == AuctionStatus.RUNNING || item.getStatus() == AuctionStatus.FINISHED) {
            throw new ValidationException("This item can only be edited before it starts or while it is in OPEN status.");
        }

        validateAuctionDraft(name, startPrice, startTime, endTime);
        validateImage(imageData);

        item.setName(name);
        item.setDescription(desc);
        item.setStartPrice(startPrice);
        item.setCurrentHighestBid(startPrice);
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setCategory(category);
        item.setImageSource(normalizeOptionalText(imageSource));
        item.setImageData(imageData);
        item.setAntiSnipingExtensionCount(0);
        item.setUpdatedAt(System.currentTimeMillis());
        auctionDAO.updateAuction(item);
    }

    // Náº¿u chÆ°a cÃ³ bid thÃ¬ xÃ³a háº³n; náº¿u Ä‘Ã£ cÃ³ bid thÃ¬ chuyá»ƒn sang CANCEL Ä‘á»ƒ giá»¯ lá»‹ch sá»­.
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete auction.
    public void deleteAuction(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException {
        AuctionItem item = requireOwnedAuction(auctionId, sellerId);

        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        if (bids.isEmpty()) {
            auctionDAO.deleteAuction(auctionId);
        } else {
            cancelSettlementForItem(item);
            item.setStatus(AuctionStatus.CANCELED);
            item.setUpdatedAt(System.currentTimeMillis());
            auctionDAO.updateAuction(item);
            eventBus.publish(AuctionEvent.statusChanged(item, item.getUpdatedAt(), "Auction was cancelled by the seller."));
        }
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get auctions by seller.
    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        return auctionDAO.findAllAuctions().stream()
                .filter(item -> item.getSellerId() == sellerId)
                .collect(Collectors.toList());
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get all auctions.
    public List<AuctionItem> getAllAuctions() {
        return auctionDAO.findAllAuctions();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get bids for auction.
    public List<BidTransaction> getBidsForAuction(int auctionId) {
        return auctionDAO.findBidsByAuction(auctionId);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get all bids.
    public List<BidTransaction> getAllBids() {
        return auctionDAO.findAllBids();
    }

    // Luá»“ng Ä‘áº·t bid hiá»‡n váº«n dá»±a vÃ o trigger DB cho auto-bid/reserve, sau Ä‘Ã³ Java xá»­ lÃ½ anti-sniping vÃ  event.
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac place bid.
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

            double existingReservationCredit = item.getWinnerId() == bidderId ? item.getCurrentHighestBid() : 0.0;
            try {
                walletService.ensureSufficientAvailableBalanceForBid(bidderId, amount, existingReservationCredit);
            } catch (ValidationException ex) {
                throw new InvalidBidException(ex.getMessage());
            }

            BidTransaction bid = new BidTransaction(0, auctionId, bidderId, amount, now, "ACCEPTED");
            item.setCurrentHighestBid(amount);
            item.setWinnerId(bidderId);
            item.setUpdatedAt(now);
            auctionDAO.saveBidAndUpdateAuction(bid, item);

            // Äá»c láº¡i sau khi DB trigger cháº¡y xong Ä‘á»ƒ láº¥y winner/current bid thá»±c táº¿ má»›i nháº¥t.
            AuctionItem latestItem = auctionDAO.findAuctionById(auctionId);
            AuctionItem effectiveItem = latestItem == null ? item : latestItem;
            boolean antiSnipingExtended = applyAntiSniping(effectiveItem, now);
            if (antiSnipingExtended) {
                auctionDAO.updateAuction(effectiveItem);
            }
            eventBus.publish(AuctionEvent.bidActivity(effectiveItem, effectiveItem.getUpdatedAt()));
            if (antiSnipingExtended) {
                eventBus.publish(AuctionEvent.antiSnipingExtended(effectiveItem, effectiveItem.getUpdatedAt()));
            }
        } finally {
            lock.unlock();
        }
    }

    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac close auction manually.
    public void closeAuctionManually(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, AuctionClosedException {
        AuctionItem item = requireOwnedAuction(auctionId, sellerId);
        if (item.getStatus() == AuctionStatus.FINISHED ||
                item.getStatus() == AuctionStatus.CANCELED ||
                item.getStatus() == AuctionStatus.PAID) {
            throw new AuctionClosedException("The auction has already ended or was cancelled.");
        }

        closeAuctionAndSettle(item, System.currentTimeMillis(), "Auction closed manually.");
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac mark auction as paid.
    public void markAuctionAsPaid(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = requireOwnedAuction(auctionId, sellerId);
            if (item.getStatus() == AuctionStatus.PAID) {
                throw new ValidationException("This auction is already marked as paid.");
            }
            if (item.getStatus() != AuctionStatus.FINISHED) {
                throw new ValidationException("Only FINISHED auctions can be marked as paid.");
            }
            if (item.getWinnerId() <= 0 || item.getCurrentHighestBid() <= 0) {
                throw new ValidationException("This auction has no winning settlement to capture.");
            }

            walletService.captureReservedFunds(item.getWinnerId(), item.getCurrentHighestBid());
            item.setStatus(AuctionStatus.PAID);
            item.setUpdatedAt(System.currentTimeMillis());
            auctionDAO.updateAuction(item);
            eventBus.publish(AuctionEvent.settled(item, item.getUpdatedAt(), "Seller marked the auction as paid."));
        } finally {
            lock.unlock();
        }
    }

    // DÃ nh cho flow má»›i: seller há»§y káº¿t quáº£ sau khi FINISHED hoáº·c cáº£ khi Ä‘Ã£ PAID.
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac cancel finished auction.
    public void cancelFinishedAuction(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = requireOwnedAuction(auctionId, sellerId);
            if (item.getStatus() == AuctionStatus.CANCELED) {
                throw new ValidationException("This auction is already cancelled.");
            }
            if (item.getStatus() != AuctionStatus.FINISHED && item.getStatus() != AuctionStatus.PAID) {
                throw new ValidationException("Only FINISHED or PAID auctions can cancel the result.");
            }

            cancelSettlementForItem(item);
            item.setStatus(AuctionStatus.CANCELED);
            item.setUpdatedAt(System.currentTimeMillis());
            auctionDAO.updateAuction(item);
            eventBus.publish(AuctionEvent.settled(item, item.getUpdatedAt(), "Seller cancelled the auction result."));
        } finally {
            lock.unlock();
        }
    }

    // Scheduler gá»i Ä‘á»‹nh ká»³ Ä‘á»ƒ Ä‘áº©y tráº¡ng thÃ¡i OPEN/RUNNING/FINISHED vÃ  xá»­ lÃ½ Ä‘Ã³ng phiÃªn.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac refresh auction statuses.
    public void refreshAuctionStatuses() {
        long now = System.currentTimeMillis();
        for (AuctionItem item : auctionDAO.findAllAuctions()) {
            AuctionStatus currentStatus = item.getStatus();
            if (currentStatus == AuctionStatus.OPEN &&
                    now >= item.getStartTime() &&
                    now < item.getEndTime()) {
                item.setStatus(AuctionStatus.RUNNING);
                item.setUpdatedAt(now);
                auctionDAO.updateAuction(item);
                eventBus.publish(AuctionEvent.statusChanged(item, now, "Auction is now running."));
                continue;
            }

            if ((currentStatus == AuctionStatus.OPEN || currentStatus == AuctionStatus.RUNNING) &&
                    now >= item.getEndTime()) {
                closeAuctionAndSettle(item, now, "Auction reached its closing time.");
            }
        }
    }

    // DÃ¹ng chung cho má»i action seller Ä‘á»ƒ kiá»ƒm tra auction tá»“n táº¡i vÃ  Ä‘Ãºng chá»§ sá»Ÿ há»¯u.
    // Phuong thuc: thuc hien chuc nang require owned auction trong lop AuctionService.
    private AuctionItem requireOwnedAuction(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) {
            throw new ItemNotFoundException("Auction item not found.");
        }
        if (item.getSellerId() != sellerId) {
            throw new UnauthorizedException("Only the creator can modify this auction.");
        }
        return item;
    }

    // Gom validate cÆ¡ báº£n cho create/update Ä‘á»ƒ trÃ¡nh láº·p.
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac validate auction draft.
    private void validateAuctionDraft(String name, double startPrice, long startTime, long endTime) throws ValidationException {
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
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac find latest bid timestamp.
    private long findLatestBidTimestamp(List<BidTransaction> bids) {
        long latestTimestamp = -1;
        for (BidTransaction bid : bids) {
            if (bid.getTimestamp() > latestTimestamp) {
                latestTimestamp = bid.getTimestamp();
            }
        }
        return latestTimestamp;
    }

    // Náº¿u bid tá»›i sÃ¡t giá» Ä‘Ã³ng thÃ¬ kÃ©o dÃ i thÃªm má»™t khoáº£ng cá»‘ Ä‘á»‹nh.
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply anti sniping.
    private boolean applyAntiSniping(AuctionItem item, long now) {
        if (item.getStatus() != AuctionStatus.RUNNING) {
            return false;
        }

        long remaining = item.getEndTime() - now;
        if (remaining > AuctionRules.ANTI_SNIPING_WINDOW_MS) {
            return false;
        }

        if (item.getAntiSnipingExtensionCount() >= AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS) {
            return false;
        }

        long extendedEndTime = Math.max(item.getEndTime(), now + AuctionRules.ANTI_SNIPING_WINDOW_MS);
        if (extendedEndTime == item.getEndTime()) {
            return false;
        }

        item.setEndTime(extendedEndTime);
        item.setAntiSnipingExtensionCount(item.getAntiSnipingExtensionCount() + 1);
        item.setUpdatedAt(Math.max(item.getUpdatedAt() + 1, now));
        return true;
    }
    // Phuong thuc: thuc hien chuc nang normalize optional text trong lop AuctionService.
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac validate image.
    private void validateImage(byte[] imageData) throws ValidationException {
        if (imageData != null && imageData.length > AuctionRules.MAX_IMAGE_BYTES) {
            throw new ValidationException("Image file is too large. Maximum supported size is 5 MB.");
        }
    }

    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac cancel settlement for item.
    private void cancelSettlementForItem(AuctionItem item) {
        if (item.getWinnerId() <= 0 || item.getCurrentHighestBid() <= 0) {
            return;
        }

        try {
            if (item.getStatus() == AuctionStatus.PAID) {
                walletService.refundCapturedFunds(item.getWinnerId(), item.getCurrentHighestBid());
            } else {
                walletService.releaseReservedFunds(item.getWinnerId(), item.getCurrentHighestBid());
            }
        } catch (ItemNotFoundException | ValidationException ex) {
            System.err.println("Wallet cancellation error for auction " + item.getId() + ": " + ex.getMessage());
        }
    }

    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac close auction and settle.
    private void closeAuctionAndSettle(AuctionItem item, long now, String summary) {
        AuctionStatus finalStatus = AuctionStatus.FINISHED;
        if (item.getWinnerId() > 0 && item.getCurrentHighestBid() > 0) {
            try {
                walletService.captureReservedFunds(item.getWinnerId(), item.getCurrentHighestBid());
                finalStatus = AuctionStatus.PAID;
            } catch (ItemNotFoundException | ValidationException ex) {
                System.err.println("Wallet settlement error for auction " + item.getId() + ": " + ex.getMessage());
            }
        }

        item.setStatus(finalStatus);
        item.setEndTime(now);
        item.setUpdatedAt(now);
        auctionDAO.updateAuction(item);
        eventBus.publish(AuctionEvent.settled(item, now, buildSettlementSummary(item, finalStatus, summary)));
    }

    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac build settlement summary.
    private String buildSettlementSummary(AuctionItem item, AuctionStatus finalStatus, String baseSummary) {
        if (finalStatus == AuctionStatus.PAID) {
            return baseSummary + " Winning funds were captured automatically.";
        }
        if (item.getWinnerId() > 0 && item.getCurrentHighestBid() > 0) {
            return baseSummary + " Winning result is waiting for seller settlement.";
        }
        return baseSummary + " Auction ended without a winning bidder.";
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
