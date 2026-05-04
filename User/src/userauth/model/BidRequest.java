package userauth.model;

import java.io.Serial;
import java.io.Serializable;

public class BidRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final int bidderId;
    private final double amount;
    private final long requestTime;

    public BidRequest(int auctionId, int bidderId, double amount) {
        this(auctionId, bidderId, amount, System.currentTimeMillis());
    }

    public BidRequest(int auctionId, int bidderId, double amount, long requestTime) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.requestTime = requestTime;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public double getAmount() {
        return amount;
    }

    public long getRequestTime() {
        return requestTime;
    }
}
