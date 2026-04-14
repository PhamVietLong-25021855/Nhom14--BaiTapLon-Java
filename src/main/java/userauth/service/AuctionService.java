package userauth.service;

import userauth.dao.AuctionDAO;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class AuctionService {
    private final AuctionDAO auctionDAO;
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks;

    public AuctionService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
        this.auctionLocks = new ConcurrentHashMap<>();
    }

    private ReentrantLock getLockForAuction(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
    }

    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime, String category, int sellerId) throws ValidationException {
        if (name == null || name.trim().isEmpty()) throw new ValidationException("Tên sản phẩm không được rỗng.");
        if (startPrice <= 0) throw new ValidationException("Giá khởi điểm phải lớn hơn 0.");
        if (startTime >= endTime) throw new ValidationException("Thời gian bắt đầu phải trước thời gian kết thúc.");
        if (endTime <= System.currentTimeMillis()) throw new ValidationException("Không thể tạo phiên đã hết hạn.");

        int newId = 1;
        for (AuctionItem item : auctionDAO.findAllAuctions()) {
            if (item.getId() >= newId) newId = item.getId() + 1;
        }

        AuctionItem item = new AuctionItem(newId, name, desc, startPrice, startTime, endTime, category, sellerId);
        auctionDAO.saveAuction(item);
    }

    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice, long startTime, long endTime, String category) throws ItemNotFoundException, UnauthorizedException, ValidationException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) throw new ItemNotFoundException("Không tìm thấy sản phẩm đấu giá.");
        if (item.getSellerId() != sellerId) throw new UnauthorizedException("Chỉ người tạo mới được sửa sản phẩm.");
        
        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        if (!bids.isEmpty()) {
            throw new ValidationException("Sản phẩm đã có người trả giá, không thể sửa đổi thông tin.");
        }
        if (item.getStatus() == AuctionStatus.RUNNING || item.getStatus() == AuctionStatus.FINISHED) {
            throw new ValidationException("Chỉ có thể sửa khi phiên chưa chạy hoặc đang ở trạng thái OPEN.");
        }

        if (name == null || name.trim().isEmpty()) throw new ValidationException("Tên sản phẩm không được rỗng.");
        if (startPrice <= 0) throw new ValidationException("Giá khởi điểm phải lớn hơn 0.");
        if (startTime >= endTime) throw new ValidationException("Thời gian bắt đầu phải trước thời gian kết thúc.");

        item.setName(name);
        item.setDescription(desc);
        item.setStartPrice(startPrice);
        item.setCurrentHighestBid(startPrice); // Because no bids yet
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setCategory(category);
        item.setUpdatedAt(System.currentTimeMillis());

        auctionDAO.updateAuction(item);
    }

    public void deleteAuction(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) throw new ItemNotFoundException("Không tìm thấy sản phẩm đấu giá.");
        if (item.getSellerId() != sellerId) throw new UnauthorizedException("Chỉ người tạo mới được xóa/hủy sản phẩm.");

        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        if (bids.isEmpty()) {
            auctionDAO.deleteAuction(auctionId); // Xóa cứng
        } else {
            item.setStatus(AuctionStatus.CANCELED); // Xóa mềm / Hủy
            item.setUpdatedAt(System.currentTimeMillis());
            auctionDAO.updateAuction(item);
        }
    }

    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        return auctionDAO.findAllAuctions().stream()
                .filter(a -> a.getSellerId() == sellerId)
                .collect(Collectors.toList());
    }

    public List<AuctionItem> getActiveAuctions() {
        return auctionDAO.findAllAuctions().stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList());
    }
    
    public List<AuctionItem> getAllAuctions() {
        return auctionDAO.findAllAuctions();
    }

    public AuctionItem getAuctionItem(int auctionId) {
        return auctionDAO.findAuctionById(auctionId);
    }

    public List<BidTransaction> getBidsForAuction(int auctionId) {
        return auctionDAO.findBidsByAuction(auctionId);
    }

    public void placeBid(int auctionId, int bidderId, double amount) throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) throw new ItemNotFoundException("Không tìm thấy sản phẩm đấu giá.");
            if (item.getStatus() != AuctionStatus.RUNNING) throw new AuctionClosedException("Phiên đấu giá không ở trạng thái đang diễn ra.");

            long now = System.currentTimeMillis();
            if (now < item.getStartTime() || now > item.getEndTime()) {
                throw new AuctionClosedException("Thời gian hiện tại không hợp lệ để đặt giá.");
            }

            if (amount <= item.getCurrentHighestBid()) {
                throw new InvalidBidException("Số tiền trả phải cao hơn giá hiện tại (" + item.getCurrentHighestBid() + ").");
            }

            int bidId = (int)(System.currentTimeMillis() % Integer.MAX_VALUE);
            if (bidId < 0) bidId = -bidId;
            
            BidTransaction bid = new BidTransaction(bidId, auctionId, bidderId, amount, now);
            auctionDAO.saveBid(bid);

            item.setCurrentHighestBid(amount);
            item.setWinnerId(bidderId);
            item.setUpdatedAt(now);
            auctionDAO.updateAuction(item);
            
        } finally {
            lock.unlock();
        }
    }

    public void closeAuctionManually(int auctionId, int sellerId) throws ItemNotFoundException, UnauthorizedException, AuctionClosedException {
        AuctionItem item = auctionDAO.findAuctionById(auctionId);
        if (item == null) throw new ItemNotFoundException("Không tìm thấy sản phẩm.");
        if (item.getSellerId() != sellerId) throw new UnauthorizedException("Bạn không có quyền đóng phiên đấu giá này.");
        if (item.getStatus() == AuctionStatus.FINISHED || item.getStatus() == AuctionStatus.CANCELED || item.getStatus() == AuctionStatus.PAID) {
            throw new AuctionClosedException("Phiên đấu giá đã kết thúc hoặc hủy.");
        }

        item.setStatus(AuctionStatus.FINISHED);
        item.setEndTime(System.currentTimeMillis());
        item.setUpdatedAt(System.currentTimeMillis());
        auctionDAO.updateAuction(item);
    }

    public void refreshAuctionStatuses() {
        long now = System.currentTimeMillis();
        for (AuctionItem item : auctionDAO.findAllAuctions()) {
            AuctionStatus currentStatus = item.getStatus();

            // Bỏ qua nếu đã kết thúc, đã thanh toán hoặc đã hủy
            if (currentStatus == AuctionStatus.FINISHED ||
                currentStatus == AuctionStatus.PAID ||
                currentStatus == AuctionStatus.CANCELED) {
                continue;
            }

            // Chuyển sang RUNNING khi đến thời gian bắt đầu
            if (currentStatus == AuctionStatus.OPEN &&
                now >= item.getStartTime() &&
                now < item.getEndTime()) {
                item.setStatus(AuctionStatus.RUNNING);
                item.setUpdatedAt(now);
                auctionDAO.updateAuction(item);
                continue;
            }

            // Chuyển sang FINISHED khi đã quá thời gian kết thúc
            if ((currentStatus == AuctionStatus.OPEN || currentStatus == AuctionStatus.RUNNING) &&
                now >= item.getEndTime()) {
                item.setStatus(AuctionStatus.FINISHED);
                item.setEndTime(now);
                item.setUpdatedAt(now);
                auctionDAO.updateAuction(item);
            }
        }
    }
}
