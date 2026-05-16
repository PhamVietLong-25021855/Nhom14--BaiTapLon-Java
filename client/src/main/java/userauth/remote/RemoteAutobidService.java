package userauth.client.remote;

import userauth.api.AutobidApi;
import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;
import userauth.network.NetworkActions;

import java.util.List;

/** AutobidService chạy ở client: gọi Server thay vì DAO. */
public class RemoteAutobidService implements AutobidApi {
    private final RemoteAuctionClient client;

    public RemoteAutobidService(RemoteAuctionClient client) {
        this.client = client;
    }

    @Override
    public void createAutobid(int bidderId, int auctionId, double maxPrice, double increment) throws ValidationException {
        try {
            String result = (String) client.call(NetworkActions.AUTOBID_CREATE, "bidderId", bidderId, "auctionId", auctionId, "maxPrice", maxPrice, "increment", increment);
            if (!"SUCCESS".equals(result)) throw new ValidationException(result);
        } catch (RemoteServerException ex) {
            // Map all server-side errors to ValidationException to satisfy interface contract
            throw new ValidationException(ex.getMessage());
        }
    }

    @Override
    public void updateAutobid(int bidderId, int id, double maxPrice, double increment)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        try {
            String result = (String) client.call(NetworkActions.AUTOBID_UPDATE, "bidderId", bidderId, "id", id, "maxPrice", maxPrice, "increment", increment);
            if (!"SUCCESS".equals(result)) throw new ValidationException(result);
        } catch (RemoteServerException ex) {
            if ("ItemNotFoundException".equals(ex.getErrorType())) {
                throw new ItemNotFoundException(ex.getMessage());
            }
            if ("UnauthorizedException".equals(ex.getErrorType())) {
                throw new UnauthorizedException(ex.getMessage());
            }
            throw new ValidationException(ex.getMessage());
        }
    }

    @Override
    public void deleteAutobid(int bidderId, int id) throws ItemNotFoundException, UnauthorizedException {
        try {
            String result = (String) client.call(NetworkActions.AUTOBID_DELETE, "bidderId", bidderId, "id", id);
            if (!"SUCCESS".equals(result)) throw new UnauthorizedException(result);
        } catch (RemoteServerException ex) {
            if ("ItemNotFoundException".equals(ex.getErrorType())) {
                throw new ItemNotFoundException(ex.getMessage());
            }
            if ("UnauthorizedException".equals(ex.getErrorType())) {
                throw new UnauthorizedException(ex.getMessage());
            }
            throw new UnauthorizedException(ex.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AutoBid> getAutobidByBidder(int bidderId) {
        try {
            return (List<AutoBid>) client.call(NetworkActions.AUTOBID_BY_BIDDER, "bidderId", bidderId);
        } catch (RemoteServerException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public AutoBid getAutobid(int id) {
        try {
            return (AutoBid) client.call(NetworkActions.AUTOBID_BY_ID, "id", id);
        } catch (RemoteServerException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }
}
