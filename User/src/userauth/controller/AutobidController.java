package userauth.controller;

import userauth.exception.*;
import userauth.model.*;
import userauth.service.AutobidService;

import java.util.List;

public class AutobidController {
    private final AutobidService autobidService;

    public AutobidController(AutobidService autobidService) {
        this.autobidService = autobidService;
    }

    public String createAutobid(int bidderId, int auctionId, double maxPrice, double increment) {
        try {
            autobidService.createAutobid( bidderId, auctionId,  maxPrice,  increment);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }

    public String updateAutobid(int bidderId,int id, double maxPrice, double increment) {
        try {
            autobidService.updateAutobid(bidderId, id , maxPrice, increment);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException | ValidationException e) {
            return e.getMessage();
        }
    }

    public String deleteAutoBid(int bidderId ,int id) {
        try {
            autobidService.deleteAutobid(bidderId, id);
            return "SUCCESS";
        } catch (ItemNotFoundException | UnauthorizedException e) {
            return e.getMessage();
        }
    }

    public List<AutoBid> getAutobidByBidder(int bidderId) {
        return autobidService.getAutobidByBidder(bidderId);
    }

    public AutoBid getAutobidById (int id){
        return  autobidService.getAutobid(id);
    }
}
