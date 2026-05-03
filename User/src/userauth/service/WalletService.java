package userauth.service;

import userauth.dao.WalletDAO;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;
import userauth.model.PaymentMethod;
import userauth.model.TopUpStatus;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;

import java.util.List;

public class WalletService {
    private static final double MIN_TOPUP_AMOUNT = 10_000;
    private static final double MAX_TOPUP_AMOUNT = 100_000_000;

    private final WalletDAO walletDAO;

    public WalletService(WalletDAO walletDAO) {
        this.walletDAO = walletDAO;
    }

    public void initializeWalletForUser(int userId) throws ValidationException {
        if (walletDAO.findWalletByUserId(userId) != null) {
            throw new ValidationException("Wallet already exists for user ID: " + userId);
        }
        createWallet(userId);
    }

    public Wallet getWallet(int userId) throws ItemNotFoundException {
        Wallet wallet = walletDAO.findWalletByUserId(userId);
        if (wallet == null) {
            throw new ItemNotFoundException("Wallet not found for user ID: " + userId);
        }
        return wallet;
    }

    public int createTopUpRequest(int userId, double amount, PaymentMethod method)
            throws ItemNotFoundException, ValidationException {
        validateTopUpAmount(amount);
        ensureWalletExists(userId);

        TopUpTransaction transaction = new TopUpTransaction(userId, amount, method);
        int transactionId = walletDAO.saveTopUpTransaction(transaction);
        if (transactionId == -1) {
            throw new IllegalStateException("Failed to create top-up transaction for user ID: " + userId);
        }

        try {
            confirmTopUp(transactionId, "AUTO_CONFIRMED");
        } catch (ItemNotFoundException | ValidationException | RuntimeException ex) {
            rollbackTopUp(transactionId);
            throw ex;
        }

        return transactionId;
    }

    public void confirmTopUp(int transactionId, String reference) throws ItemNotFoundException, ValidationException {
        TopUpTransaction transaction = requireTopUpTransaction(transactionId);
        ensurePendingTransaction(transaction, transactionId, "confirmed");

        Wallet wallet = getWallet(transaction.getUserID());

        transaction.setStatus(TopUpStatus.SUCCESS);
        transaction.setReferenceCode(reference);
        transaction.setCompleteAt(System.currentTimeMillis());
        walletDAO.updateTopUpTransaction(transaction);

        wallet.setBalance(wallet.getBalance() + transaction.getAmount());
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    public void cancelTopUp(int transactionId) throws ItemNotFoundException, ValidationException {
        TopUpTransaction transaction = requireTopUpTransaction(transactionId);
        ensurePendingTransaction(transaction, transactionId, "cancelled");
        transaction.setStatus(TopUpStatus.CANCELLED);
        walletDAO.updateTopUpTransaction(transaction);
    }

    public void deductFromWallet(int userId, double amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount);

        Wallet wallet = getWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new ValidationException("Insufficient balance in wallet for user ID: " + userId);
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    public void addToWallet(int userId, double amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount);

        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    public List<TopUpTransaction> getTopUpHistory(int userId) throws ItemNotFoundException {
        getWallet(userId);
        return walletDAO.findTopUpTransactionsByUserId(userId);
    }

    public List<TopUpTransaction> getAllPendingTransactions() {
        return walletDAO.findAllPendingTransactions();
    }

    private Wallet createWallet(int userId) {
        Wallet wallet = new Wallet(userId);
        int walletId = walletDAO.saveWallet(wallet);
        if (walletId == -1) {
            throw new IllegalStateException("Failed to create wallet for user ID: " + userId);
        }
        wallet.setId(walletId);
        return wallet;
    }

    private Wallet ensureWalletExists(int userId) {
        Wallet wallet = walletDAO.findWalletByUserId(userId);
        return wallet != null ? wallet : createWallet(userId);
    }

    private TopUpTransaction requireTopUpTransaction(int transactionId) throws ItemNotFoundException {
        TopUpTransaction transaction = walletDAO.findTopUpTransactionById(transactionId);
        if (transaction == null) {
            throw new ItemNotFoundException("Top-up transaction not found for ID: " + transactionId);
        }
        return transaction;
    }

    private void ensurePendingTransaction(TopUpTransaction transaction, int transactionId, String action)
            throws ValidationException {
        if (transaction.getStatus() != TopUpStatus.PENDING) {
            throw new ValidationException(
                    "Only PENDING transactions can be " + action + " for ID: " + transactionId
            );
        }
    }

    private void rollbackTopUp(int transactionId) {
        try {
            cancelTopUp(transactionId);
        } catch (ItemNotFoundException | ValidationException ignored) {
        }
    }

    private void validateTopUpAmount(double amount) throws ValidationException {
        if (amount < MIN_TOPUP_AMOUNT) {
            throw new ValidationException("Top-up amount must be at least " + MIN_TOPUP_AMOUNT);
        }
        if (amount > MAX_TOPUP_AMOUNT) {
            throw new ValidationException("Top-up amount must not exceed " + MAX_TOPUP_AMOUNT);
        }
        if (amount % 1000 != 0) {
            throw new ValidationException("Top-up amount must be a multiple of 1000 VND");
        }
    }

    private void validatePositiveAmount(double amount) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("Amount must be greater than 0");
        }
    }
}
