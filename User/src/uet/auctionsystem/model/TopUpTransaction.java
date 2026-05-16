package uet.auctionsystem.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop TopUpTransaction; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class TopUpTransaction extends Entity {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho user id.
    private int userId;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho amount.
    private double amount;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho method.
    private PaymentMethod method;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho status.
    private TopUpStatus status;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho reference code.
    private String referenceCode;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho transaction time.
    private long transactionTime;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho complete at.
    private Long completeAt;
    // Ham tao: khoi tao doi tuong TopUpTransaction voi cac phu thuoc can thiet.
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
    // Ham tao: khoi tao doi tuong TopUpTransaction voi cac phu thuoc can thiet.
    public TopUpTransaction(int userId, double amount, PaymentMethod method) {
        this(0, userId, amount, method, TopUpStatus.PENDING, null, System.currentTimeMillis(), null);
    }
    // Ham tao: khoi tao doi tuong TopUpTransaction voi cac phu thuoc can thiet.
    public TopUpTransaction(int userId, double amount, PaymentMethod method, String referenceCode) {
        this(0, userId, amount, method, TopUpStatus.PENDING, referenceCode, System.currentTimeMillis(), null);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get user id.
    public int getUserId() {
        return userId;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get amount.
    public double getAmount() {
        return amount;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get method.
    public PaymentMethod getMethod() {
        return method;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get status.
    public TopUpStatus getStatus() {
        return status;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get reference code.
    public String getReferenceCode() {
        return referenceCode;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get transaction time.
    public long getTransactionTime() {
        return transactionTime;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get complete at.
    public Long getCompleteAt() {
        return completeAt;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set user id.
    public void setUserId(int userId) {
        this.userId = userId;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set amount.
    public void setAmount(double amount) {
        this.amount = amount;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set method.
    public void setMethod(PaymentMethod method) {
        this.method = method;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set status.
    public void setStatus(TopUpStatus status) {
        this.status = status;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set reference code.
    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set transaction time.
    public void setTransactionTime(long transactionTime) {
        this.transactionTime = transactionTime;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set complete at.
    public void setCompleteAt(Long completeAt) {
        this.completeAt = completeAt;
    }
}
