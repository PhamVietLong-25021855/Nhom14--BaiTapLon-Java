package uet.auctionsystem.dao;

import uet.auctionsystem.model.TopUpTransaction;
import uet.auctionsystem.model.Wallet;
import java.util.List;

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao giao dien WalletDAO; phu trach hop dong hoac truy cap du lieu cho database.
public interface WalletDAO {
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save wallet.
    int saveWallet(Wallet wallet);
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update wallet.
    void updateWallet(Wallet wallet);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find wallet by user id.
    Wallet findWalletByUserId(int userId);
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete wallet.
    void deleteWallet(int walletId);
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save top up transaction.
    int saveTopUpTransaction(TopUpTransaction topUpTransaction);
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update top up transaction.
    void updateTopUpTransaction(TopUpTransaction transaction);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find top up transaction by id.
    TopUpTransaction findTopUpTransactionById(int transactionId);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find top up transactions by user id.
    List<TopUpTransaction> findTopUpTransactionsByUserId(int userId);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all pending transactions.
    List<TopUpTransaction> findAllPendingTransactions();
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete top up transaction.
    void deleteTopUpTransaction(int transactionId);
}
