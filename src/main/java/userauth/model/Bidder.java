package userauth.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Bidder kế thừa User (→ Entity).
 * Đại diện cho người đấu giá trong hệ thống.
 * Thể hiện: Inheritance, Polymorphism (override printInfo), Encapsulation
 */
public class Bidder extends User {
    private List<Integer> bidHistory; // Danh sách ID các giao dịch đặt giá

    public Bidder(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.BIDDER, status, createdAt, updatedAt);
        this.bidHistory = new ArrayList<>();
    }

    // Getter / Setter
    public List<Integer> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<Integer> bidHistory) { this.bidHistory = bidHistory; }

    public void addBidToHistory(int bidTransactionId) {
        bidHistory.add(bidTransactionId);
    }

    public int getTotalBids() {
        return bidHistory.size();
    }

    // Polymorphism: override printInfo()
    @Override
    public void printInfo() {
        System.out.println("=== THÔNG TIN NGƯỜI ĐẤU GIÁ ===");
        System.out.println("ID: " + getId());
        System.out.println("Username: " + getUsername());
        System.out.println("Họ tên: " + getFullName());
        System.out.println("Email: " + getEmail());
        System.out.println("Vai trò: " + getRoleName());
        System.out.println("Trạng thái: " + getStatus());
        System.out.println("Tổng lượt đặt giá: " + bidHistory.size());
    }
}
