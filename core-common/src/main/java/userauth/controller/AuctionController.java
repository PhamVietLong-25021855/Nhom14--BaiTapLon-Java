package userauth.controller;

import userauth.api.AuctionApi;
import userauth.exception.AuctionClosedException;
import userauth.exception.InvalidBidException;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.model.Role;
import userauth.model.User;

import java.util.List;
import java.util.Map;

public class AuctionController {
    private final AuctionApi auctionService;

    public AuctionController(AuctionApi auctionService) {
        this.auctionService = auctionService;
    }

    public String createAuction(String name, String desc, double startPrice, long startTime, long endTime,
                              String category, String imageSource, byte[] imageData, int sellerId) {
        try {
            auctionService.createAuction(name, desc, startPrice, startTime, endTime, category, imageSource, imageData, sellerId);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public String updateAuction(int auctionId, int sellerId, String name, String desc, double startPrice,
                              long startTime, long endTime, String category, String imageSource, byte[] imageData) {
        try {
            auctionService.updateAuction(auctionId, sellerId, name, desc, startPrice, startTime, endTime, category, imageSource, imageData);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String deleteAuction(int auctionId, int sellerId) {
        try {
            auctionService.deleteAuction(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public String deleteAuctionAsAdmin(User currentUser, int auctionId) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "Only an admin can delete this auction.";
        }
        try {
            auctionService.deleteAuctionAsAdmin(auctionId);
            return "SUCCESS";
        } catch (ItemNotFoundException e) {
            return e.getMessage();
        }
    }

    public AuctionItem getAuctionById(int auctionId) throws ItemNotFoundException {
        return auctionService.getAuctionById(auctionId);
    }

    public List<AuctionItem> getAuctionsBySeller(int sellerId) {
        return auctionService.getAuctionsBySeller(sellerId);
    }

    public List<AuctionItem> getAllAuctions() {
        return auctionService.getAllAuctions();
    }

    public List<AuctionItem> getAllAuctionSummaries() {
        return auctionService.getAllAuctionSummaries();
    }

    public List<BidTransaction> getBidsForAuction(int auctionId) {
        return auctionService.getBidsForAuction(auctionId);
    }

    public List<BidTransaction> getAllBids() {
        return auctionService.getAllBids();
    }

    public int countAllBids() {
        return auctionService.countAllBids();
    }

    public Map<Integer, Integer> getBidCounts() {
        return auctionService.getBidCounts();
    }

    public String placeBid(int auctionId, int bidderId, double amount) {
        try {
            auctionService.placeBid(auctionId, bidderId, amount);
            return "SUCCESS";
        } catch (ItemNotFoundException | AuctionClosedException | InvalidBidException e) {
            return e.getMessage();
        }
    }

    public String closeAuction(int auctionId, int sellerId) {
        try {
            auctionService.closeAuctionManually(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | AuctionClosedException e) {
            return e.getMessage();
        }
    }

    public String markAuctionAsPaid(int auctionId, int sellerId) {
        try {
            auctionService.markAuctionAsPaid(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String cancelFinishedAuction(int auctionId, int sellerId) {
        try {
            auctionService.cancelFinishedAuction(auctionId, sellerId);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String startAdminEarlyCloseCountdown(User currentUser, int auctionId) {
        try {
            auctionService.startAdminEarlyCloseCountdown(auctionId);
            return "SUCCESS";
        } catch (ItemNotFoundException | AuctionClosedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String cancelAdminEarlyCloseCountdown(User currentUser, int auctionId) {
        try {
            auctionService.cancelAdminEarlyCloseCountdown(auctionId);
            return "SUCCESS";
        } catch (ItemNotFoundException | ValidationException e) {
            return e.getMessage();
        }
    }

    public Map<Integer, Integer> getAdminEarlyCloseCountdowns() {
        return auctionService.getAdminEarlyCloseCountdowns();
    }

    public String refreshStatuses() {
        try {
            auctionService.refreshAuctionStatuses();
            return "SUCCESS";
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
