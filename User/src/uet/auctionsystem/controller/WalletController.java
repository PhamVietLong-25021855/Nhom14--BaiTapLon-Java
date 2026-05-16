package uet.auctionsystem.controller;

import uet.auctionsystem.exception.ItemNotFoundException;
import uet.auctionsystem.exception.ValidationException;
import uet.auctionsystem.model.PaymentMethod;
import uet.auctionsystem.model.TopUpTransaction;
import uet.auctionsystem.model.Wallet;
import uet.auctionsystem.service.WalletService;
import java.util.List;

// Ghi chu file: File controller nam giua giao dien va service; nhan lenh tu UI va goi nghiep vu tuong ung.
// Khai bao lop WalletController; dieu phoi thao tac UI va chuyen tiep yeu cau xu ly nghiep vu.
public class WalletController {
    // Thuoc tinh: giu tham chieu den WalletService de phoi hop xu ly.
    private final WalletService walletService;
    // Ham tao: khoi tao doi tuong WalletController voi cac phu thuoc can thiet.
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize wallet.
    public String initializeWallet(int userId) {
        try {
            walletService.initializeWalletForUser(userId);
            return "SUCCESS";
        } catch (ValidationException ex) {
            return "ERROR: " + ex.getMessage();
        }
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create top up request.
    public String createTopUpRequest(int userId, double amount, PaymentMethod method) {
        try {
            int transactionId = walletService.createTopUpRequest(userId, amount, method);
            return "SUCCESS: Transaction ID " + transactionId;
        } catch (Exception ex) {
            return "ERROR: " + ex.getMessage();
        }
    }
    // Phuong thuc: thuc hien chuc nang confirm top up trong lop WalletController.
    public String confirmTopUp(int transactionId, String reference) {
        try {
            walletService.confirmTopUp(transactionId, reference);
            return "SUCCESS";
        } catch (ValidationException | ItemNotFoundException ex) {
            return "ERROR: " + ex.getMessage();
        }
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac cancel top up.
    public String cancelTopUp(int transactionId) {
        try {
            walletService.cancelTopUp(transactionId);
            return "SUCCESS";
        } catch (ValidationException | ItemNotFoundException ex) {
            return "ERROR: " + ex.getMessage();
        }
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get wallet.
    public Wallet getWallet(int userId) {
        try {
            return walletService.getWallet(userId);
        } catch (ItemNotFoundException ex) {
            return null;
        }
    }
    // Phuong thuc: thuc hien chuc nang deduct from wallet trong lop WalletController.
    public String deductFromWallet(int userId, double amount) {
        try {
            walletService.deductFromWallet(userId, amount);
            return "SUCCESS";
        } catch (ValidationException | ItemNotFoundException ex) {
            return "ERROR: " + ex.getMessage();
        }
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add to wallet.
    public String addToWallet(int userId, double amount) {
        try {
            walletService.addToWallet(userId, amount);
            return "SUCCESS";
        } catch (ValidationException | ItemNotFoundException ex) {
            return "ERROR: " + ex.getMessage();
        }
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get top up history.
    public List<TopUpTransaction> getTopUpHistory(int userId) {
        try {
            return walletService.getTopUpHistory(userId);
        } catch (ItemNotFoundException ex) {
            return List.of();
        }
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get all pending transactions.
    public List<TopUpTransaction> getAllPendingTransactions() {
        return walletService.getAllPendingTransactions();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get wallet balance.
    public double getWalletBalance(int userId) {
        Wallet wallet = getWallet(userId);
        return wallet == null ? 0.0 : wallet.getBalance();
    }
}
