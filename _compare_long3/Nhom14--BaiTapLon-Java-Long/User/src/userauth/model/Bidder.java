package userauth.model;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {
    private List<Integer> bidHistory;

    public Bidder(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.BIDDER, status, createdAt, updatedAt);
        this.bidHistory = new ArrayList<>();
    }

    public List<Integer> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<Integer> bidHistory) {
        this.bidHistory = bidHistory;
    }

    public void addBidToHistory(int bidTransactionId) {
        bidHistory.add(bidTransactionId);
    }

    public int getTotalBids() {
        return bidHistory.size();
    }
}
