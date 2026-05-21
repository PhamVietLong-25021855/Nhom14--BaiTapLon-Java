package userauth.model;

public class WalletTransaction extends Entity {
    private int userId;
    private WalletTransactionType type;
    private long amount;
    private Integer auctionId;
    private String reference;
    private long createdAt;

    public WalletTransaction() {
        super();
    }

    public WalletTransaction(int id, int userId, WalletTransactionType type, long amount,
                            Integer auctionId, String reference, long createdAt) {
        super(id);
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
        this.reference = reference;
        this.createdAt = createdAt;
    }

    public WalletTransaction(int userId, WalletTransactionType type, long amount,
                            Integer auctionId, String reference) {
        this(0, userId, type, amount, auctionId, reference, System.currentTimeMillis());
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public WalletTransactionType getType() {
        return type;
    }

    public void setType(WalletTransactionType type) {
        this.type = type;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Integer getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Integer auctionId) {
        this.auctionId = auctionId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
