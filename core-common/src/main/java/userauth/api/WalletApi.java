package userauth.api;

import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;
import userauth.model.PaymentMethod;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;

import java.util.List;

public interface WalletApi {
    Wallet getWallet(int userId) throws ItemNotFoundException;

    int createTopUpRequest(int userId, double amount, PaymentMethod method)
            throws ItemNotFoundException, ValidationException;

    List<TopUpTransaction> getTopUpHistory(int userId) throws ItemNotFoundException;
}
