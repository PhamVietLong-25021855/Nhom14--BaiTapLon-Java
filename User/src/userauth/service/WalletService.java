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
    private static final double BALANCE_EPSILON = 0.0001;

    private final WalletDAO walletDAO;

    public WalletService(WalletDAO walletDAO) {
        this.walletDAO = walletDAO;
    }

    public void initializeWalletForUser(int userId) throws ValidationException {
        if (walletDAO.findWalletByUserId(userId) != null) {
            throw new ValidationException("Wallet already exists for user ID: " + userId);
        }
        walletDAO.saveWallet(new Wallet(userId));
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

        // DucAnh2 modeled top-up as an immediate simulated confirmation rather than a real payment callback.
        confirmTopUp(transactionId, "AUTO_CONFIRMED");
        return transactionId;
    }

    public void confirmTopUp(int transactionId, String reference)
            throws ItemNotFoundException, ValidationException {
        TopUpTransaction transaction = walletDAO.findTopUpTransactionById(transactionId);
        if (transaction == null) {
            throw new ItemNotFoundException("Top-up transaction not found for ID: " + transactionId);
        }
        if (transaction.getStatus() != TopUpStatus.PENDING) {
            throw new ValidationException("Top-up transaction is not in PENDING status for ID: " + transactionId);
        }

        transaction.setStatus(TopUpStatus.SUCCESS);
        transaction.setReferenceCode(reference);
        transaction.setCompleteAt(System.currentTimeMillis());
        walletDAO.updateTopUpTransaction(transaction);

        Wallet wallet = getWallet(transaction.getUserId());
        wallet.setBalance(wallet.getBalance() + transaction.getAmount());
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

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

    public void deductFromWallet(int userId, double amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Amount");
        Wallet wallet = getWallet(userId);
        if (wallet.getAvailableBalance() + BALANCE_EPSILON < amount) {
            throw new ValidationException("Insufficient available wallet balance for user ID: " + userId);
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    public void addToWallet(int userId, double amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Amount");
        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    public void ensureSufficientAvailableBalanceForBid(int userId, double targetBidAmount, double existingReservationCredit)
            throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(targetBidAmount, "Bid amount");

        Wallet wallet = getWallet(userId);
        double usableBalance = wallet.getAvailableBalance() + Math.max(existingReservationCredit, 0.0);
        if (usableBalance + BALANCE_EPSILON < targetBidAmount) {
            throw new ValidationException(
                    "Insufficient available wallet balance. Available funds for this bid: " +
                            formatMoney(usableBalance) + "."
            );
        }
    }

    public void releaseReservedFunds(int userId, double amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Reserved amount");

        Wallet wallet = getWallet(userId);
        if (wallet.getReservedBalance() + BALANCE_EPSILON < amount) {
            throw new ValidationException("Reserved wallet balance is lower than the amount to release for user ID: " + userId);
        }

        wallet.setReservedBalance(Math.max(0.0, wallet.getReservedBalance() - amount));
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }

    public void captureReservedFunds(int userId, double amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Reserved amount");

        Wallet wallet = getWallet(userId);
        if (wallet.getReservedBalance() + BALANCE_EPSILON < amount) {
            throw new ValidationException("Reserved wallet balance is lower than the amount to capture for user ID: " + userId);
        }

        wallet.setReservedBalance(Math.max(0.0, wallet.getReservedBalance() - amount));
        wallet.setBalance(wallet.getBalance() - amount);
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

    private void ensureWalletExists(int userId) {
        if (walletDAO.findWalletByUserId(userId) != null) {
            return;
        }
        walletDAO.saveWallet(new Wallet(userId));
    }

    private void validateTopUpAmount(double amount) throws ValidationException {
        if (amount < MIN_TOPUP_AMOUNT) {
            throw new ValidationException("Top-up amount must be at least " + (long) MIN_TOPUP_AMOUNT + ".");
        }
        if (amount > MAX_TOPUP_AMOUNT) {
            throw new ValidationException("Top-up amount must not exceed " + (long) MAX_TOPUP_AMOUNT + ".");
        }
        if (amount % 1000 != 0) {
            throw new ValidationException("Top-up amount must be a multiple of 1000 VND.");
        }
    }

    private void validatePositiveAmount(double amount, String label) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException(label + " must be greater than 0.");
        }
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f VND", amount);
    }
}
