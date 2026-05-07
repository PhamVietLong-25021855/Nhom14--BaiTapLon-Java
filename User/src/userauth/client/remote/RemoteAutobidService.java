package userauth.client.remote;

import userauth.exception.ItemNotFoundException;
import userauth.exception.UnauthorizedException;
import userauth.exception.ValidationException;
import userauth.model.AutoBid;
import userauth.network.NetworkActions;
import userauth.service.AutobidService;

import java.util.List;

/** AutobidService chạy ở client: gọi Server thay vì DAO. */
public class RemoteAutobidService extends AutobidService {
    private final RemoteAuctionClient client;

    public RemoteAutobidService(RemoteAuctionClient client) {
        super(new NoOpDaos.AutoBidDao());
        this.client = client;
    }

    @Override
    public void createAutobid(int bidderId, int auctionId, double maxPrice, double increment) throws ValidationException {
        String result = result(NetworkActions.AUTOBID_CREATE, "bidderId", bidderId, "auctionId", auctionId, "maxPrice", maxPrice, "increment", increment);
        if (!"SUCCESS".equals(result)) throw new ValidationException(result);
    }

    @Override
    public void updateAutobid(int bidderId, int id, double maxPrice, double increment)
            throws ItemNotFoundException, UnauthorizedException, ValidationException {
        String result = result(NetworkActions.AUTOBID_UPDATE, "bidderId", bidderId, "id", id, "maxPrice", maxPrice, "increment", increment);
        if (!"SUCCESS".equals(result)) throw new ValidationException(result);
    }

    @Override
    public void deleteAutobid(int bidderId, int id) throws ItemNotFoundException, UnauthorizedException {
        String result = result(NetworkActions.AUTOBID_DELETE, "bidderId", bidderId, "id", id);
        if (!"SUCCESS".equals(result)) throw new UnauthorizedException(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AutoBid> getAutobidByBidder(int bidderId) {
        return (List<AutoBid>) client.call(NetworkActions.AUTOBID_BY_BIDDER, "bidderId", bidderId);
    }

    @Override
    public AutoBid getAutobid(int id) {
        return (AutoBid) client.call(NetworkActions.AUTOBID_BY_ID, "id", id);
    }

    private String result(String action, Object... keyValues) {
        return (String) client.call(action, keyValues);
    }
}
