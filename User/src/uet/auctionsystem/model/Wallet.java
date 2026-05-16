package uet.auctionsystem.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop Wallet; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class Wallet extends Entity {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho user id.
    private int userId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho balance.
    private double balance;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho reserved balance.
    private double reservedBalance;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho created at.
    private long createdAt;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho updated at.
    private long updatedAt;
    // Ham tao: khoi tao doi tuong Wallet voi cac phu thuoc can thiet.
    public Wallet(int id, int userId, double balance, double reservedBalance, long createdAt, long updatedAt) {
        super(id);
        this.userId = userId;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    // Ham tao: khoi tao doi tuong Wallet voi cac phu thuoc can thiet.
    public Wallet(int userId) {
        this(0, userId, 0.0, 0.0, System.currentTimeMillis(), System.currentTimeMillis());
    }
    // Ham tao: khoi tao doi tuong Wallet voi cac phu thuoc can thiet.
    public Wallet(int userId, double balance) {
        this(0, userId, balance, 0.0, System.currentTimeMillis(), System.currentTimeMillis());
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get user id.
    public int getUserId() {
        return userId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get balance.
    public double getBalance() {
        return balance;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get reserved balance.
    public double getReservedBalance() {
        return reservedBalance;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get available balance.
    public double getAvailableBalance() {
        return balance - reservedBalance;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get created at.
    public long getCreatedAt() {
        return createdAt;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get updated at.
    public long getUpdatedAt() {
        return updatedAt;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set user id.
    public void setUserId(int userId) {
        this.userId = userId;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set balance.
    public void setBalance(double balance) {
        this.balance = balance;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set reserved balance.
    public void setReservedBalance(double reservedBalance) {
        this.reservedBalance = reservedBalance;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set created at.
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set updated at.
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
