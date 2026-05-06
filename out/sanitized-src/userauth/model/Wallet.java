package userauth.model;

// File note: Model ví của bidder; tách số dư khả dụng và số dư đang reserve.
public class Wallet extends Entity {
    private int userId;
    private double balance;
    private double reservedBalance;
    private long createdAt;
    private long updatedAt;

    public Wallet(int id, int userId, double balance, double reservedBalance, long createdAt, long updatedAt) {
        super(id);
        this.userId = userId;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Wallet(int userId) {
        this(0, userId, 0.0, 0.0, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public Wallet(int userId, double balance) {
        this(0, userId, balance, 0.0, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public int getUserId() {
        return userId;
    }

    public double getBalance() {
        return balance;
    }

    public double getReservedBalance() {
        return reservedBalance;
    }

    public double getAvailableBalance() {
        return balance - reservedBalance;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setReservedBalance(double reservedBalance) {
        this.reservedBalance = reservedBalance;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

