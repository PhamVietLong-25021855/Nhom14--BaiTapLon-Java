package userauth.api;

import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;

import java.util.List;

public interface AutobidApi {
    void createAutobid(int bidderId, int auctionId, double maxPrice, double increment)
            throws ValidationException;

    void updateAutobid(int bidderId, int id, double maxPrice, double increment)
            throws ItemNotFoundException, UnauthorizedException, ValidationException;

    void deleteAutobid(int bidderId, int id) throws ItemNotFoundException, UnauthorizedException;

    List<AutoBid> getAutobidByBidder(int bidderId);

    AutoBid getAutobid(int id);
}
