package userauth.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Seller kế thừa User (→ Entity).
 * Đại diện cho người bán trong hệ thống đấu giá.
 * Thể hiện: Inheritance, Polymorphism (override printInfo), Encapsulation
 */
public class Seller extends User {
    private List<Integer> auctionIds; // Danh sách ID các sản phẩm đấu giá đã tạo

    public Seller(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.SELLER, status, createdAt, updatedAt);
        this.auctionIds = new ArrayList<>();
    }

    // Getter / Setter
    public List<Integer> getAuctionIds() { return auctionIds; }
    public void setAuctionIds(List<Integer> auctionIds) { this.auctionIds = auctionIds; }

    public void addAuction(int auctionId) {
        auctionIds.add(auctionId);
    }

    public int getTotalAuctions() {
        return auctionIds.size();
    }

    // Polymorphism: override printInfo()
    @Override
    public void printInfo() {
        System.out.println("=== THÔNG TIN NGƯỜI BÁN ===");
        System.out.println("ID: " + getId());
        System.out.println("Username: " + getUsername());
        System.out.println("Họ tên: " + getFullName());
        System.out.println("Email: " + getEmail());
        System.out.println("Vai trò: " + getRoleName());
        System.out.println("Trạng thái: " + getStatus());
        System.out.println("Tổng sản phẩm đấu giá: " + auctionIds.size());
    }
}
