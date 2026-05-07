package userauth.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho người tham gia đấu giá.
 *
 * Bidder kế thừa User và luôn có Role.BIDDER.
 * Ngoài thông tin tài khoản, Bidder có thêm bidHistory để lưu danh sách id
 * của các giao dịch đặt giá mà người này đã thực hiện trong phiên chạy hiện tại.
 */
public class Bidder extends User {
    /**
     * Danh sách id của các BidTransaction mà bidder đã tham gia.
     * Lưu ý: danh sách này chỉ là dữ liệu model trong bộ nhớ; lịch sử thật thường được lưu ở bảng bid_transactions.
     */
    private List<Integer> bidHistory;

    /**
     * Constructor tạo một người đấu giá.
     *
     * @param id id tài khoản bidder
     * @param username tên đăng nhập
     * @param password mật khẩu đã hash
     * @param fullName họ tên bidder
     * @param email email bidder
     * @param status trạng thái tài khoản
     * @param createdAt thời điểm tạo
     * @param updatedAt thời điểm cập nhật
     */
    public Bidder(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.BIDDER, status, createdAt, updatedAt);
        this.bidHistory = new ArrayList<>();
    }

    /** @return danh sách id giao dịch đặt giá của bidder */
    public List<Integer> getBidHistory() {
        return bidHistory;
    }

    /** @param bidHistory danh sách lịch sử đặt giá cần gán lại */
    public void setBidHistory(List<Integer> bidHistory) {
        this.bidHistory = bidHistory;
    }

    /**
     * Thêm một giao dịch đặt giá vào lịch sử của bidder.
     *
     * Hàm này nhận id của BidTransaction, không nhận trực tiếp object BidTransaction.
     * Cách này giúp model nhẹ hơn và tránh lưu trùng toàn bộ dữ liệu giao dịch.
     *
     * @param bidTransactionId id giao dịch đặt giá vừa tạo
     */
    public void addBidToHistory(int bidTransactionId) {
        bidHistory.add(bidTransactionId);
    }

    /**
     * Đếm tổng số lượt đặt giá được lưu trong bidHistory.
     *
     * @return số lượng giao dịch bidder đã tham gia theo danh sách hiện tại
     */
    public int getTotalBids() {
        return bidHistory.size();
    }
}
