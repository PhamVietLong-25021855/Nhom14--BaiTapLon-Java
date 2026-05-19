package userauth.remote;

import userauth.api.WalletApi;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;
import userauth.model.PaymentMethod;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;
import userauth.network.NetworkActions;

import java.util.List;

public class RemoteWalletService implements WalletApi {
    private final RemoteAuctionClient client;

    public RemoteWalletService(RemoteAuctionClient client) {
        this.client = client;
    }

    @Override
    public Wallet getWallet(int userId) throws ItemNotFoundException {
        try {
            return (Wallet) client.call(NetworkActions.WALLET_GET, "userId", userId);
        } catch (RemoteServerException ex) {
            throw new ItemNotFoundException(ex.getMessage());
        }
    }

    @Override
    public int createTopUpRequest(int userId, long amount, PaymentMethod method)
            throws ItemNotFoundException, ValidationException {
        try {
            String result = (String) client.call(
                    NetworkActions.WALLET_TOP_UP,
                    "userId", userId,
                    "amount", amount,
                    "method", method
            );
            if (result == null || !result.startsWith("SUCCESS: Transaction ID ")) {
                throw new ValidationException(result == null ? "Unable to create top-up request." : result);
            }
            return Integer.parseInt(result.substring("SUCCESS: Transaction ID ".length()).trim());
        } catch (RemoteServerException ex) {
            if ("ItemNotFoundException".equals(ex.getErrorType())) {
                throw new ItemNotFoundException(ex.getMessage());
            }
            throw new ValidationException(ex.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TopUpTransaction> getTopUpHistory(int userId) throws ItemNotFoundException {
        try {
            return (List<TopUpTransaction>) client.call(NetworkActions.WALLET_TOP_UP_HISTORY, "userId", userId);
        } catch (RemoteServerException ex) {
            throw new ItemNotFoundException(ex.getMessage());
        }
    }
}
