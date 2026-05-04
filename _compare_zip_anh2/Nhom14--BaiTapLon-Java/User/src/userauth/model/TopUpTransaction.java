package userauth.model;

public class TopUpTransaction extends Entity{
    private int userID;
    private double amount;
    private PaymentMethod method; //CREDIT_CARD, BANK_TRANSFER, E_WALLET, CASH;
    private TopUpStatus status; //PENDING, COMPLETED, FAILED, CANCELLED;
    private String referenceCode;
    private long transactionTime;
    private Long completeAt; // chưa complete thì null ;

    //constructor 1 ;
    public TopUpTransaction(int id, int userID, double amount, PaymentMethod method, TopUpStatus status, String referenceCode, long transactionTime, Long completeAt) {
        super(id);
        this.userID = userID;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.referenceCode = referenceCode;
        this.transactionTime = transactionTime;
        this.completeAt = completeAt;
    }
    //contructor 2 : new transaction;
    public TopUpTransaction(int userID, double amount, PaymentMethod method) {
        this(0, userID, amount, method, TopUpStatus.PENDING, null, System.currentTimeMillis(), null);
    }
    //contructor 3 : khi đã có mã trả về ;
    public TopUpTransaction(int userID, double amount, PaymentMethod method, String referenceCode){
        this(0, userID, amount, method, TopUpStatus.PENDING, referenceCode, System.currentTimeMillis(), null);
    }

    public int getUserID(){return userID;}
    public double getAmount(){return amount;}
    public PaymentMethod getMethod(){return method;}
    public TopUpStatus getStatus(){return status;}
    public String getReferenceCode(){return referenceCode;}
    public long getTransactionTime(){return transactionTime;}
    public Long getCompleteAt(){return completeAt;}

    public void setUserID(int userID){this.userID = userID;}
    public void setAmount(double amount){this.amount = amount;}
    public void setMethod(PaymentMethod method){this.method = method;}
    public void setStatus(TopUpStatus status){this.status = status;}
    public void setReferenceCode(String referenceCode){this.referenceCode = referenceCode;}
    public void setTransactionTime(long transactionTime){this.transactionTime = transactionTime;}
    public void setCompleteAt(Long completeAt){this.completeAt = completeAt;}

    @Override
    public String toString(){
        return id + "," + userID + "," + amount + "," + method.name() + "," + status.name() + "," + referenceCode + "," + transactionTime + "," + completeAt;
    }

}