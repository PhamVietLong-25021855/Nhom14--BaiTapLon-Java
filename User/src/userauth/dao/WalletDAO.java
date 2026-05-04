package userauth.dao;

import userauth.model.TopUpTransaction;
import userauth.model.Wallet;

import java.util.List;

public interface WalletDAO {
    int saveWallet(Wallet wallet);

    void updateWallet(Wallet wallet);

    Wallet findWalletByUserId(int userId);

    void deleteWallet(int walletId);

    int saveTopUpTransaction(TopUpTransaction topUpTransaction);

    void updateTopUpTransaction(TopUpTransaction transaction);

    TopUpTransaction findTopUpTransactionById(int transactionId);

    List<TopUpTransaction> findTopUpTransactionsByUserId(int userId);

    List<TopUpTransaction> findAllPendingTransactions();

    void deleteTopUpTransaction(int transactionId);
}
