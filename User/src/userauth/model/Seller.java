package userauth.model;

import java.util.ArrayList;
import java.util.List;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop Seller; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class Seller extends User {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho auction ids.
    private List<Integer> auctionIds;
    // Ham tao: khoi tao doi tuong Seller voi cac phu thuoc can thiet.
    public Seller(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.SELLER, status, createdAt, updatedAt);
        this.auctionIds = new ArrayList<>();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get auction ids.
    public List<Integer> getAuctionIds() {
        return auctionIds;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auction ids.
    public void setAuctionIds(List<Integer> auctionIds) {
        this.auctionIds = auctionIds;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add auction.
    public void addAuction(int auctionId) {
        auctionIds.add(auctionId);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get total auctions.
    public int getTotalAuctions() {
        return auctionIds.size();
    }
}
