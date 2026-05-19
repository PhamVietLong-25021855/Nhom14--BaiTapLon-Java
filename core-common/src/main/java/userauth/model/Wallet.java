package userauth.model;

public class Wallet extends Entity {
    private int userId;
    private long balance;
    private long reservedBalance;
    private long createdAt;
    private long updatedAt;

    public Wallet() {
        super();
    }

    public Wallet(int id, int userId, long balance, long reservedBalance, long createdAt, long updatedAt) {
        super(id);
        this.userId = userId;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Wallet(int userId) {
        this(0, userId, 0L, 0L, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public long getReservedBalance() {
        return reservedBalance;
    }

    public void setReservedBalance(long reservedBalance) {
        this.reservedBalance = reservedBalance;
    }

    public long getAvailableBalance() {
        return balance - reservedBalance;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
