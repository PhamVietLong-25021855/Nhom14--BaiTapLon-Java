package userauth.model;

public class TopUpTransaction extends Entity {
    private int userId;
    private double amount;
    private PaymentMethod method;
    private TopUpStatus status;
    private String referenceCode;
    private long transactionTime;
    private Long completeAt;

    public TopUpTransaction(
            int id,
            int userId,
            double amount,
            PaymentMethod method,
            TopUpStatus status,
            String referenceCode,
            long transactionTime,
            Long completeAt
    ) {
        super(id);
        this.userId = userId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.referenceCode = referenceCode;
        this.transactionTime = transactionTime;
        this.completeAt = completeAt;
    }

    public TopUpTransaction(int userId, double amount, PaymentMethod method) {
        this(0, userId, amount, method, TopUpStatus.PENDING, null, System.currentTimeMillis(), null);
    }

    public TopUpTransaction(int userId, double amount, PaymentMethod method, String referenceCode) {
        this(0, userId, amount, method, TopUpStatus.PENDING, referenceCode, System.currentTimeMillis(), null);
    }

    public int getUserId() {
        return userId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public TopUpStatus getStatus() {
        return status;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public long getTransactionTime() {
        return transactionTime;
    }

    public Long getCompleteAt() {
        return completeAt;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public void setStatus(TopUpStatus status) {
        this.status = status;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public void setTransactionTime(long transactionTime) {
        this.transactionTime = transactionTime;
    }

    public void setCompleteAt(Long completeAt) {
        this.completeAt = completeAt;
    }
}
