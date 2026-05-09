package userauth.service;

import userauth.api.AuctionApi;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Lớp xử lý nghiệp vụ chính của hệ thống đấu giá.
 *
 * AuctionService không trực tiếp hiển thị giao diện, mà đứng giữa Controller và DAO:
 * - Nhận yêu cầu từ phía giao diện/controller.
 * - Kiểm tra điều kiện hợp lệ của phiên đấu giá, người bán, người đặt giá.
 * - Gọi DAO để đọc/ghi dữ liệu xuống database.
 * - Phát sự kiện qua AuctionEventBus để các phần khác của hệ thống có thể cập nhật giao diện/thông báo.
 *
 * Các thao tác nhạy cảm như đặt giá, chốt phiên, đếm ngược đóng sớm được khóa theo từng auctionId
 * bằng ReentrantLock để tránh lỗi khi nhiều người thao tác cùng lúc.
 */
public class AuctionService implements AuctionApi {
    private final AuctionDAO auctionDAO;
    private final AutoBidDAO autoBidDAO;
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks;
    private final ConcurrentHashMap<Integer, AdminEarlyCloseState> adminEarlyCloseStates;
    private final AuctionSettlementHandlerFactory settlementHandlerFactory;
    private final AuctionEventBus eventBus;

    /**
     * Khởi tạo AuctionService với các DAO cần thiết.
     *
     * @param auctionDAO DAO dùng để thao tác dữ liệu phiên đấu giá và lịch sử đặt giá.
     * @param autoBidDAO DAO dùng để thao tác dữ liệu đấu giá tự động.
     */
    public AuctionService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO) {
        this.auctionDAO = auctionDAO;
        this.autoBidDAO = autoBidDAO;
        this.auctionLocks = new ConcurrentHashMap<>();
        this.adminEarlyCloseStates = new ConcurrentHashMap<>();
        this.settlementHandlerFactory = new AuctionSettlementHandlerFactory();
        this.eventBus = AuctionEventBus.getInstance();
    }

    /**
     * Lấy lock riêng cho một phiên đấu giá.
     *
     * Mỗi auctionId có một ReentrantLock riêng để các phiên đấu giá khác nhau vẫn có thể chạy song song,
     * nhưng các thao tác trên cùng một phiên sẽ được xử lý tuần tự để tránh tranh chấp dữ liệu.
     *
     * @param auctionId id của phiên đấu giá.
     * @return lock tương ứng với phiên đấu giá đó.
     */
    private ReentrantLock getLockForAuction(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, ignored -> new ReentrantLock());
    }

    /**
     * Tạo một phiên đấu giá mới.
     *
     * Hàm này kiểm tra tên sản phẩm, giá khởi điểm, thời gian bắt đầu/kết thúc và dung lượng ảnh.
     * Nếu hợp lệ, dữ liệu được đóng gói vào AuctionItem rồi lưu xuống database thông qua AuctionDAO.
     *
     * @param name tên sản phẩm đấu giá.
     * @param desc mô tả sản phẩm.
     * @param startPrice giá khởi điểm, bắt buộc lớn hơn 0.
     * @param startTime thời gian bắt đầu phiên, tính bằng milliseconds.
     * @param endTime thời gian kết thúc phiên, tính bằng milliseconds.
     * @param category danh mục sản phẩm.
     * @param imageSource đường dẫn hoặc nguồn ảnh dạng text, có thể rỗng.
     * @param imageData dữ liệu ảnh dạng byte array, có thể null.
     * @param sellerId id của người bán tạo phiên đấu giá.
     * @throws ValidationException khi dữ liệu đầu vào không hợp lệ.
     */
    public void createAuction(String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, byte[] imageData, int sellerId)
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
        validateImage(imageData);

        AuctionItem item = new AuctionItem(0, name, desc, startPrice, startTime, endTime, category, normalizeOptionalText(imageSource), imageData, sellerId);
        auctionDAO.saveAuction(item);
    }

    /**
     * Cập nhật thông tin một phiên đấu giá do seller tạo.
     *
     * Chỉ người tạo phiên mới được sửa. Phiên đã có lượt đặt giá hoặc đã RUNNING/FINISHED sẽ không được sửa
     * để tránh làm thay đổi điều kiện đấu giá sau khi người dùng đã tham gia.
     *
     * @param auctionId id phiên đấu giá cần sửa.
     * @param sellerId id người bán đang thực hiện thao tác.
     * @param name tên sản phẩm mới.
     * @param desc mô tả mới.
     * @param startPrice giá khởi điểm mới.
     * @param startTime thời gian bắt đầu mới.
     * @param endTime thời gian kết thúc mới.
     * @param category danh mục mới.
     * @param imageSource nguồn/đường dẫn ảnh mới.
     * @param imageData dữ liệu ảnh mới.
     * @throws ItemNotFoundException khi không tìm thấy phiên đấu giá.
     * @throws UnauthorizedException khi người sửa không phải chủ phiên.
     * @throws ValidationException khi phiên không được phép sửa hoặc dữ liệu không hợp lệ.
     */
    public void updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice, long startTime, long endTime, String category, String imageSource, byte[] imageData)
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

    /**
     * Xóa hoặc hủy một phiên đấu giá.
     *
     * Nếu phiên chưa có lượt đặt giá, dữ liệu sẽ bị xóa khỏi database.
     * Nếu phiên đã có lượt đặt giá, hệ thống không xóa cứng mà chuyển trạng thái sang CANCELED
     * để vẫn giữ được lịch sử giao dịch.
     *
     * @param auctionId id phiên đấu giá cần xóa/hủy.
     * @param sellerId id người bán yêu cầu thao tác.
     * @throws ItemNotFoundException khi không tìm thấy phiên đấu giá.
     * @throws UnauthorizedException khi người thao tác không phải chủ phiên.
     */
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
            eventBus.publish(AuctionEvent.statusChanged(item, item.getUpdatedAt(), "Auction was cancelled by the seller."));
        }
        adminEarlyCloseStates.remove(auctionId);
    }

    /**
     * Lấy danh sách các phiên đấu giá thuộc về một seller cụ thể.
     *
     * @param sellerId id người bán.
     * @return danh sách AuctionItem do seller này tạo.
     */
    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        return auctionDAO.findAllAuctions().stream()
                .filter(item -> item.getSellerId() == sellerId)
                .collect(Collectors.toList());
    }

    /**
     * Lấy toàn bộ phiên đấu giá trong hệ thống.
     *
     * @return danh sách tất cả AuctionItem.
     */
    public List<AuctionItem> getAllAuctions() {

        return auctionDAO.findAllAuctions();
    }

    /**
     * Lấy lịch sử đặt giá của một phiên đấu giá.
     *
     * @param auctionId id phiên đấu giá.
     * @return danh sách các giao dịch đặt giá của phiên đó.
     */
    public List<BidTransaction> getBidsForAuction(int auctionId) {
        return auctionDAO.findBidsByAuction(auctionId);
    }

    /**
     * Lấy toàn bộ lịch sử đặt giá trong hệ thống.
     *
     * @return danh sách tất cả BidTransaction.
     */
    public List<BidTransaction> getAllBids() {
        return auctionDAO.findAllBids();
    }

    /**
     * Thực hiện đặt giá cho một phiên đấu giá đang chạy.
     *
     * Đây là hàm quan trọng nhất của quá trình đấu giá. Hàm sẽ:
     * 1. Khóa phiên đấu giá để tránh nhiều người đặt giá cùng lúc gây sai dữ liệu.
     * 2. Kiểm tra phiên có tồn tại, đang RUNNING và còn trong thời gian cho phép hay không.
     * 3. Kiểm tra số tiền đặt phải lớn hơn giá khởi điểm và giá hiện tại.
     * 4. Lưu giao dịch đặt giá mới.
     * 5. Cập nhật giá cao nhất và người thắng tạm thời.
     * 6. Kích hoạt đấu giá tự động nếu có người dùng cài auto-bid.
     * 7. Áp dụng cơ chế chống đặt giá sát giờ, nếu lượt đặt nằm trong khoảng cuối phiên.
     * 8. Phát sự kiện để giao diện hoặc listener khác cập nhật trạng thái.
     *
     * @param auctionId id phiên đấu giá.
     * @param bidderId id người đặt giá.
     * @param amount số tiền người dùng đặt.
     * @throws ItemNotFoundException khi không tìm thấy phiên đấu giá.
     * @throws AuctionClosedException khi phiên không chạy hoặc ngoài thời gian đặt giá.
     * @throws InvalidBidException khi số tiền đặt không hợp lệ.
     */
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

            long eventTime = now;
            auctionDAO.saveBid(new BidTransaction(0, auctionId, bidderId, amount, eventTime, "ACCEPTED"));

            item.setCurrentHighestBid(amount);
            item.setWinnerId(bidderId);
            item.setUpdatedAt(eventTime);
            eventTime = applyAutoBids(item, eventTime);
            boolean antiSnipingExtended = applyAntiSniping(item, now, eventTime);
            auctionDAO.updateAuction(item);
            refreshEarlyCloseSnapshot(auctionId, item, now);
            eventBus.publish(AuctionEvent.bidActivity(item, item.getUpdatedAt()));
            if (antiSnipingExtended) {
                eventBus.publish(AuctionEvent.antiSnipingExtended(item, item.getUpdatedAt()));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cho phép seller đóng phiên đấu giá thủ công.
     *
     * Hàm này chỉ dùng khi seller muốn kết thúc phiên trước thời điểm kết thúc tự nhiên.
     * Phiên đã FINISHED, CANCELED hoặc PAID thì không thể đóng lại.
     *
     * @param auctionId id phiên đấu giá cần đóng.
     * @param sellerId id seller yêu cầu đóng phiên.
     * @throws ItemNotFoundException khi không tìm thấy phiên đấu giá.
     * @throws UnauthorizedException khi người thao tác không phải chủ phiên.
     * @throws AuctionClosedException khi phiên đã kết thúc/hủy/thanh toán.
     */
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
        eventBus.publish(AuctionEvent.statusChanged(item, item.getUpdatedAt(), "Auction closed manually."));
    }

    /**
     * Bắt đầu cơ chế đếm ngược đóng sớm bởi admin.
     *
     * Khi admin bật chức năng này, hệ thống ghi lại số lượt bid, giá cao nhất và thời điểm bid mới nhất.
     * Nếu trong quá trình đếm ngược có bid mới hoặc giá thay đổi, bộ đếm sẽ được reset.
     * Nếu hết bộ đếm mà không có thay đổi, phiên sẽ tự động chuyển sang FINISHED.
     *
     * @param auctionId id phiên đấu giá cần kích hoạt đóng sớm.
     * @throws ItemNotFoundException khi không tìm thấy phiên đấu giá.
     * @throws AuctionClosedException khi phiên không ở trạng thái RUNNING.
     * @throws ValidationException khi phiên đã có countdown đóng sớm.
     */
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

    /**
     * Hủy cơ chế đếm ngược đóng sớm của admin.
     *
     * @param auctionId id phiên đấu giá cần hủy countdown.
     * @throws ItemNotFoundException khi không tìm thấy phiên đấu giá.
     * @throws ValidationException khi phiên chưa bật countdown đóng sớm.
     */
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

    /**
     * Lấy trạng thái countdown đóng sớm hiện tại của các phiên đấu giá.
     *
     * @return Map có key là auctionId, value là số nhịp đếm còn lại.
     */
    public Map<Integer, Integer> getAdminEarlyCloseCountdowns() {
        Map<Integer, Integer> countdowns = new HashMap<>();
        adminEarlyCloseStates.forEach((auctionId, state) -> countdowns.put(auctionId, state.remainingCounts));
        return countdowns;
    }

    /**
     * Đánh dấu phiên đấu giá đã hoàn tất thanh toán.
     *
     * Chỉ áp dụng cho phiên đã FINISHED và seller là người tạo phiên.
     *
     * @param auctionId id phiên đấu giá.
     * @param sellerId id người bán.
     * @throws ItemNotFoundException khi không tìm thấy phiên.
     * @throws UnauthorizedException khi người thao tác không phải seller của phiên.
     * @throws ValidationException khi phiên chưa đủ điều kiện chuyển sang PAID.
     */
    public void markAuctionAsPaid(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        settleFinishedAuction(auctionId, sellerId, AuctionStatus.PAID);
    }

    /**
     * Hủy một phiên đấu giá đã kết thúc nhưng không hoàn tất giao dịch.
     *
     * Hàm này dùng chung cơ chế settleFinishedAuction, nhưng targetStatus là CANCELED.
     *
     * @param auctionId id phiên đấu giá.
     * @param sellerId id người bán.
     * @throws ItemNotFoundException khi không tìm thấy phiên.
     * @throws UnauthorizedException khi người thao tác không phải seller của phiên.
     * @throws ValidationException khi phiên chưa ở trạng thái FINISHED.
     */
    public void cancelFinishedAuction(int auctionId, int sellerId)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        settleFinishedAuction(auctionId, sellerId, AuctionStatus.CANCELED);
    }

    /**
     * Cập nhật trạng thái các phiên đấu giá dựa trên thời gian hiện tại.
     *
     * Hàm này thường được gọi định kỳ. Logic chính:
     * - OPEN -> RUNNING khi đến thời gian bắt đầu.
     * - OPEN/RUNNING -> FINISHED khi quá thời gian kết thúc.
     * - Bỏ qua các phiên đã FINISHED, PAID hoặc CANCELED.
     * - Sau khi cập nhật trạng thái, tiếp tục xử lý countdown đóng sớm của admin.
     */
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
                eventBus.publish(AuctionEvent.statusChanged(item, now, "Auction is now running."));
                continue;
            }

            if ((currentStatus == AuctionStatus.OPEN || currentStatus == AuctionStatus.RUNNING) &&
                    now >= item.getEndTime()) {
                item.setStatus(AuctionStatus.FINISHED);
                item.setEndTime(now);
                item.setUpdatedAt(now);
                auctionDAO.updateAuction(item);
                adminEarlyCloseStates.remove(item.getId());
                eventBus.publish(AuctionEvent.statusChanged(item, now, "Auction has finished."));
            }
        }

        tickAdminEarlyCloseCountdowns(now);
    }

    /**
     * Giảm bộ đếm đóng sớm của admin theo từng nhịp thời gian.
     *
     * Mỗi phiên có countdown sẽ được kiểm tra khoảng 1 giây một lần.
     * Nếu có lượt đặt giá mới, số lượng bid thay đổi hoặc giá cao nhất thay đổi, countdown được reset.
     * Nếu bộ đếm về 0 mà không có thay đổi, phiên được đóng và chuyển sang FINISHED.
     *
     * @param now thời điểm hiện tại tính bằng milliseconds.
     */
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
                    item.setStatus(AuctionStatus.FINISHED);
                    item.setEndTime(now);
                    item.setUpdatedAt(now);
                    auctionDAO.updateAuction(item);
                    adminEarlyCloseStates.remove(auctionId);
                    eventBus.publish(AuctionEvent.statusChanged(item, now, "Auction finished after the admin early-close countdown."));
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Làm mới dữ liệu quan sát của countdown đóng sớm sau khi có hoạt động đặt giá.
     *
     * Việc cập nhật snapshot giúp countdown không đóng phiên ngay khi vừa có người đặt giá mới.
     *
     * @param auctionId id phiên đấu giá.
     * @param item đối tượng phiên đấu giá hiện tại.
     * @param now thời điểm cập nhật.
     */
    private void refreshEarlyCloseSnapshot(int auctionId, AuctionItem item, long now) {
        AdminEarlyCloseState state = adminEarlyCloseStates.get(auctionId);
        if (state == null) {
            return;
        }

        List<BidTransaction> bids = auctionDAO.findBidsByAuction(auctionId);
        state.reset(bids.size(), item.getCurrentHighestBid(), findLatestBidTimestamp(bids), now);
    }

    /**
     * Tìm thời điểm đặt giá mới nhất trong danh sách bid.
     *
     * @param bids danh sách lịch sử đặt giá.
     * @return timestamp lớn nhất; trả về -1 nếu danh sách rỗng.
     */
    private long findLatestBidTimestamp(List<BidTransaction> bids) {
        long latestTimestamp = -1;
        for (BidTransaction bid : bids) {
            if (bid.getTimestamp() > latestTimestamp) {
                latestTimestamp = bid.getTimestamp();
            }
        }
        return latestTimestamp;
    }

    /**
     * Áp dụng đấu giá tự động sau khi có một lượt đặt giá mới.
     *
     * Hàm sẽ liên tục chọn người có auto-bid phù hợp tiếp theo và tự động tăng giá cho đến khi
     * không còn ai đủ điều kiện vượt giá hiện tại. Mỗi lần auto-bid được lưu như một BidTransaction.
     *
     * @param item phiên đấu giá đang xử lý.
     * @param eventTime thời điểm của sự kiện đặt giá ban đầu.
     * @return thời điểm sự kiện mới nhất sau khi xử lý xong toàn bộ auto-bid.
     */
    private long applyAutoBids(AuctionItem item, long eventTime) {
        List<AutoBid> autoBids = autoBidDAO.findAutoBidsByAuction(item.getId());
        if (autoBids.isEmpty()) {
            return eventTime;
        }

        long currentEventTime = eventTime;
        while (true) {
            AutoBid nextBidder = selectNextAutoBidder(autoBids, item.getWinnerId(), item.getCurrentHighestBid());
            if (nextBidder == null) {
                return currentEventTime;
            }

            double nextAmount = Math.min(item.getCurrentHighestBid() + nextBidder.getIncrement(), nextBidder.getMaxPrice());
            if (nextAmount <= item.getCurrentHighestBid()) {
                return currentEventTime;
            }

            currentEventTime++;
            auctionDAO.saveBid(new BidTransaction(0, item.getId(), nextBidder.getBidderId(), nextAmount, currentEventTime, "ACCEPTED"));
            item.setCurrentHighestBid(nextAmount);
            item.setWinnerId(nextBidder.getBidderId());
            item.setUpdatedAt(currentEventTime);
        }
    }

    /**
     * Chọn người dùng auto-bid tiếp theo có khả năng vượt giá hiện tại.
     *
     * Người đang tạm thắng sẽ không tự đấu với chính mình. Trong các auto-bid hợp lệ, hệ thống ưu tiên:
     * 1. Người có maxPrice cao hơn.
     * 2. Nếu maxPrice bằng nhau, người tạo auto-bid sớm hơn.
     * 3. Nếu vẫn bằng nhau, id auto-bid nhỏ hơn.
     *
     * @param autoBids danh sách cấu hình auto-bid của phiên.
     * @param currentWinnerId id người đang tạm thắng.
     * @param currentHighestBid giá cao nhất hiện tại.
     * @return AutoBid được chọn; null nếu không có ai đủ điều kiện.
     */
    private AutoBid selectNextAutoBidder(List<AutoBid> autoBids, int currentWinnerId, double currentHighestBid) {
        return autoBids.stream()
                .filter(autoBid -> autoBid.getBidderId() != currentWinnerId)
                .filter(autoBid -> autoBid.getMaxPrice() > currentHighestBid)
                .sorted(Comparator
                        .comparingDouble(AutoBid::getMaxPrice).reversed()
                        .thenComparingLong(AutoBid::getCreatedAt)
                        .thenComparingInt(AutoBid::getId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Áp dụng cơ chế chống đặt giá sát giờ, còn gọi là anti-sniping.
     *
     * Nếu người dùng đặt giá trong khoảng thời gian cuối phiên, hệ thống tự động kéo dài thời gian kết thúc
     * để các người dùng khác có cơ hội phản hồi. Số lần gia hạn bị giới hạn bởi AuctionRules.
     *
     * @param item phiên đấu giá đang xử lý.
     * @param now thời điểm đặt giá thực tế.
     * @param eventTime thời điểm sự kiện mới nhất sau khi lưu bid/auto-bid.
     * @return true nếu phiên được gia hạn; false nếu không cần hoặc không đủ điều kiện gia hạn.
     */
    private boolean applyAntiSniping(AuctionItem item, long now, long eventTime) {
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

        long nextEventTime = Math.max(eventTime + 1, now);
        item.setEndTime(extendedEndTime);
        item.setAntiSnipingExtensionCount(item.getAntiSnipingExtensionCount() + 1);
        item.setUpdatedAt(nextEventTime);
        return true;
    }

    /**
     * Chuẩn hóa chuỗi không bắt buộc.
     *
     * Nếu chuỗi null hoặc chỉ chứa khoảng trắng thì chuyển thành null để lưu database gọn hơn.
     *
     * @param value chuỗi cần chuẩn hóa.
     * @return chuỗi đã trim hoặc null nếu không có nội dung.
     */
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Kiểm tra dung lượng ảnh tải lên.
     *
     * @param imageData dữ liệu ảnh dạng byte array.
     * @throws ValidationException khi ảnh vượt quá dung lượng tối đa cho phép.
     */
    private void validateImage(byte[] imageData) throws ValidationException {
        if (imageData != null && imageData.length > AuctionRules.MAX_IMAGE_BYTES) {
            throw new ValidationException("Image file is too large. Maximum supported size is 5 MB.");
        }
    }

    /**
     * Xử lý trạng thái cuối của một phiên đã kết thúc.
     *
     * Hàm dùng Factory để tạo handler phù hợp với trạng thái đích, ví dụ PAID hoặc CANCELED.
     * Handler chịu trách nhiệm validate và áp dụng thay đổi vào AuctionItem.
     *
     * @param auctionId id phiên đấu giá.
     * @param sellerId id người bán.
     * @param targetStatus trạng thái muốn chuyển tới, thường là PAID hoặc CANCELED.
     * @throws ItemNotFoundException khi không tìm thấy phiên.
     * @throws UnauthorizedException khi người thao tác không phải seller của phiên.
     * @throws ValidationException khi phiên không đủ điều kiện chuyển trạng thái.
     */
    private void settleFinishedAuction(int auctionId, int sellerId, AuctionStatus targetStatus)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        ReentrantLock lock = getLockForAuction(auctionId);
        lock.lock();
        try {
            AuctionItem item = auctionDAO.findAuctionById(auctionId);
            if (item == null) {
                throw new ItemNotFoundException("Auction item not found.");
            }
            if (item.getSellerId() != sellerId) {
                throw new UnauthorizedException("Only the creator can settle this auction.");
            }
            if (item.getStatus() != AuctionStatus.FINISHED) {
                throw new ValidationException("Only finished auctions can move to PAID or CANCELED.");
            }

            AuctionSettlementHandler settlementHandler = settlementHandlerFactory.create(targetStatus);
            settlementHandler.validate(item);
            long now = System.currentTimeMillis();
            settlementHandler.apply(item, now);
            auctionDAO.updateAuction(item);
            adminEarlyCloseStates.remove(auctionId);
            eventBus.publish(AuctionEvent.settled(item, now, settlementHandler.summary(item)));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lớp nội bộ lưu snapshot phục vụ cơ chế admin đóng sớm.
     *
     * Snapshot gồm: số nhịp đếm còn lại, số lượng bid đã quan sát, giá cao nhất đã quan sát,
     * thời điểm bid mới nhất và thời điểm tick gần nhất. Dựa vào các giá trị này, hệ thống biết
     * countdown có cần reset hay có thể tiếp tục giảm.
     */
    private static final class AdminEarlyCloseState {
        private int remainingCounts;
        private int observedBidCount;
        private double observedHighestBid;
        private long observedLatestBidTimestamp;
        private long lastTickAt;

        /**
         * Tạo snapshot ban đầu từ trạng thái hiện tại của phiên đấu giá và danh sách bid.
         *
         * @param item phiên đấu giá hiện tại.
         * @param bids danh sách bid hiện có của phiên.
         * @param now thời điểm tạo snapshot.
         * @return đối tượng AdminEarlyCloseState đã được khởi tạo.
         */
        private static AdminEarlyCloseState from(AuctionItem item, List<BidTransaction> bids, long now) {
            AdminEarlyCloseState state = new AdminEarlyCloseState();
            state.reset(bids.size(), item.getCurrentHighestBid(), latestTimestamp(bids), now);
            return state;
        }

        /**
         * Reset countdown và cập nhật lại các thông tin quan sát.
         *
         * Hàm này được gọi khi bắt đầu countdown hoặc khi có hoạt động bid mới làm thay đổi phiên.
         *
         * @param bidCount số lượng bid hiện tại.
         * @param highestBid giá cao nhất hiện tại.
         * @param latestBidTimestamp timestamp của bid mới nhất.
         * @param now thời điểm reset.
         */
        private void reset(int bidCount, double highestBid, long latestBidTimestamp, long now) {
            this.remainingCounts = AuctionRules.ADMIN_EARLY_CLOSE_COUNTS;
            this.observedBidCount = bidCount;
            this.observedHighestBid = highestBid;
            this.observedLatestBidTimestamp = latestBidTimestamp;
            this.lastTickAt = now;
        }

        /**
         * Tìm timestamp mới nhất trong danh sách bid.
         *
         * @param bids danh sách bid cần kiểm tra.
         * @return timestamp lớn nhất; -1 nếu chưa có bid nào.
         */
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
}
