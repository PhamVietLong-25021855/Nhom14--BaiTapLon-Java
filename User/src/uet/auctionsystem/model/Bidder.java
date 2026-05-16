package uet.auctionsystem.model;

import java.util.ArrayList;
import java.util.List;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop Bidder; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class Bidder extends User {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho bid history.
    private List<Integer> bidHistory;
    // Ham tao: khoi tao doi tuong Bidder voi cac phu thuoc can thiet.
    public Bidder(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.BIDDER, status, createdAt, updatedAt);
        this.bidHistory = new ArrayList<>();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get bid history.
    public List<Integer> getBidHistory() {
        return bidHistory;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set bid history.
    public void setBidHistory(List<Integer> bidHistory) {
        this.bidHistory = bidHistory;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add bid to history.
    public void addBidToHistory(int bidTransactionId) {
        bidHistory.add(bidTransactionId);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get total bids.
    public int getTotalBids() {
        return bidHistory.size();
    }
}
