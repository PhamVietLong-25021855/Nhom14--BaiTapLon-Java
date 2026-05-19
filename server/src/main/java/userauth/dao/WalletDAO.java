package userauth.dao;

import userauth.exception.ValidationException;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;
import userauth.model.WalletTransaction;

import java.util.List;

public interface WalletDAO {
    int saveWallet(Wallet wallet) throws ValidationException;

    void updateWallet(Wallet wallet) throws ValidationException;

    Wallet findWalletByUserId(int userId);

    int saveTopUpTransaction(TopUpTransaction transaction);

    void updateTopUpTransaction(TopUpTransaction transaction);

    TopUpTransaction findTopUpTransactionById(int transactionId);

    List<TopUpTransaction> findTopUpTransactionsByUserId(int userId);

    void saveWalletTransaction(WalletTransaction transaction);

    List<WalletTransaction> findWalletTransactionsByUserId(int userId);
}
