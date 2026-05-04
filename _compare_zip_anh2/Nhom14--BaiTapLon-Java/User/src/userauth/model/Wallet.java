package userauth.model;

public class Wallet extends Entity{
    private int userID ;
    private double balance ;
    private long createdAt;
    private long updatedAt;

    // constructor 1 ;
    public Wallet(int id, int userID, double balance, long createdAt, long updatedAt){
        super(id);
        this.userID = userID ;
        this.balance = balance ;
        this.createdAt = createdAt ;
        this.updatedAt = updatedAt ;
    }
    // constructor 2 : new user ;
    public Wallet(int userID){
        this(0, userID, 0.0, System.currentTimeMillis(), System.currentTimeMillis());
    }
    // constructor 3 : old user ;
    public Wallet(int userID, double balance){
        this(0, userID, balance, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public int getUserID(){return userID;}
    public double getBalance(){return balance;}
    public long getCreatedAt(){return createdAt;}
    public long getUpdatedAt(){return updatedAt;}

    public void setUserID(int userID){this.userID = userID;}
    public void setBalance(double balance){this.balance = balance;}
    public void setCreatedAt(long createdAt){this.createdAt = createdAt;}
    public void setUpdatedAt(long updatedAt){this.updatedAt = updatedAt;}

    @Override
    public String toString(){
        return id + "," + userID + "," + balance + "," + createdAt + "," + updatedAt ;
    }
}
