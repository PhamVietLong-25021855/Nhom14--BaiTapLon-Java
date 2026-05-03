package userauth.model;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private List<Integer> auctionIds;

    public Seller(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.SELLER, status, createdAt, updatedAt);
        this.auctionIds = new ArrayList<>();
    }

    public List<Integer> getAuctionIds() {
        return auctionIds;
    }

    public void setAuctionIds(List<Integer> auctionIds) {
        this.auctionIds = auctionIds;
    }

    public void addAuction(int auctionId) {
        auctionIds.add(auctionId);
    }

    public int getTotalAuctions() {
        return auctionIds.size();
    }
}
