package userauth.service;

import userauth.dao.WalletDAO;
import userauth.model.*;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;

import java.util.List;

public class WalletService {
    private final WalletDAO walletDAO;
    private static final double MIN_TOPUP_AMOUNT = 10000;
    private static final double MAX_TOPUP_AMOUNT = 100000000;

    public WalletService(WalletDAO walletDAO) {
        this.walletDAO = walletDAO;
    }

    public void initializeWalletForUser(int userId) throws ValidationException {
        if (walletDAO.findWalletByUserId(userId) != null) {
            throw new ValidationException("Wallet already exists for user ID: " + userId);
        }
        Wallet newWallet = new Wallet(userId);
        int walletId = walletDAO.saveWallet(newWallet);
        if (walletId == -1) {
            throw new RuntimeException("Failed to create wallet for user ID: " + userId);
        }
    }

    public Wallet getWallet(int userId) throws ItemNotFoundException {
        Wallet wallet = walletDAO.findWalletByUserId(userId);
        if (wallet == null) {
            throw new ItemNotFoundException("Wallet not found for user ID: " + userId);
        }
        return wallet;
    }

    // tạo yêu cầu nạp tiền :
    public int createTopUpRequest(int userId, double amount, PaymentMethod method) throws ItemNotFoundException, ValidationException {
        validateTopUpAmount(amount);

        // Check if wallet exists, if not create it
        Wallet wallet = walletDAO.findWalletByUserId(userId);
        if (wallet == null) {
            Wallet newWallet = new Wallet(userId);
            int walletId = walletDAO.saveWallet(newWallet);
            if (walletId == -1) {
                throw new RuntimeException("Failed to create wallet for user ID: " + userId);
            }
        }

        // create new transaction :
        TopUpTransaction transaction = new TopUpTransaction(userId, amount, method);
        int transactionId = walletDAO.saveTopUpTransaction(transaction);

        if (transactionId == -1) {
            throw new RuntimeException("Failed to create top-up transaction for user ID: " + userId);
        }

        // Immediately confirm the top-up for instant balance update
        try {
            confirmTopUp(transactionId, "AUTO_CONFIRMED");
        } catch (Exception e) {
            // If confirmation fails, cancel the transaction
            try {
                cancelTopUp(transactionId);
            } catch (Exception cancelEx) {
                // Log or handle cancel failure
            }
            throw new RuntimeException("Failed to confirm top-up: " + e.getMessage());
        }

        return transactionId;
    }

    // tạo yêu cầu xong -> xác nhận thanh toán thành công
    public void confirmTopUp(int transactionId, String reference) throws ItemNotFoundException, ValidationException {
        // Tìm transaction theo id:
        TopUpTransaction transaction = walletDAO.findTopUpTransactionById(transactionId);
        if (transaction == null) {
            throw new ItemNotFoundException("Top-up transaction not found for ID: " + transactionId);
        }
        // kiểm tra status của transaction xem có ở PENDING ?
        if (transaction.getStatus() != TopUpStatus.PENDING) {
            throw new ValidationException("Top-up transaction is not in PENDING status for ID: " + transactionId);
        }

        // cập nhật transaction : status = COMPLETED, reference code, completeAt = now
        transaction.setStatus(TopUpStatus.SUCCESS);
        transaction.setReferenceCode(reference);
        transaction.setCompleteAt(System.currentTimeMillis());
        walletDAO.updateTopUpTransaction(transaction);

        // cập nhật balance của wallet : cộng thêm amount vào balance hiện tại
        Wallet wallet = walletDAO.findWalletByUserId(transaction.getUserID());
        wallet.setBalance(wallet.getBalance() + transaction.getAmount());
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    //Huỷ transaction nạp tiền :
    public void cancelTopUp(int transactionId) throws ItemNotFoundException, ValidationException {
        TopUpTransaction transaction = walletDAO.findTopUpTransactionById(transactionId);
        if (transaction == null) {
            throw new ItemNotFoundException("Top-up transaction not found for ID: " + transactionId);
        }

        if (transaction.getStatus() != TopUpStatus.PENDING) {
            throw new ValidationException("Only PENDING transactions can be cancelled for ID: " + transactionId);
        }

        transaction.setStatus(TopUpStatus.CANCELLED);
        walletDAO.updateTopUpTransaction(transaction);
    }

    //trừ tiền từ ví wallet :
    public void deductFromWallet(int userId,double amount) throws ItemNotFoundException , ValidationException {
        if(amount <= 0){
            throw new ValidationException("Amount must be greater than 0");
        }

        Wallet wallet = getWallet(userId);
        if(wallet.getBalance() < amount){
            throw new ValidationException("Insufficient balance in wallet for user ID: " + userId);
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    //Cộng tiền vào ví :
    public void addToWallet(int userId, double amount) throws ItemNotFoundException, ValidationException{
        if(amount <= 0){
            throw new ValidationException("Amount must be greater than 0");
        }

        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    // lịch sử nạp tiền của user :
    public List<TopUpTransaction> getTopUpHistory(int userId) throws ValidationException, ItemNotFoundException{
        getWallet(userId); // nếu không tìm thấy wallet thì ném exception ;
        return walletDAO.findTopUpTransactionsByUserId(userId);
    }

    // Lấy các transaction PENDING(dành cho admin để cancel)
    public List<TopUpTransaction> getAllPendingTransactions() throws ValidationException, ItemNotFoundException{
        getWallet(1); // nếu không tìm thấy wallet thì ném exception ;
        return walletDAO.findAllPendingTransactions();
    }

    //Validate số tiền (hàng 39);
    private void validateTopUpAmount(double amount) throws ValidationException, ItemNotFoundException{
        if(amount < MIN_TOPUP_AMOUNT){
            throw new ValidationException("Top-up amount must be at least " + MIN_TOPUP_AMOUNT);
        }
        if(amount > MAX_TOPUP_AMOUNT){
            throw new ValidationException("Top-up amount must not exceed " + MAX_TOPUP_AMOUNT);
        }
        if(amount % 1000 !=0){
            throw new ValidationException("Top-up amount must be a multiple of 1000 VNĐ");
        }
    }
}