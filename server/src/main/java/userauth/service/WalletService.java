package userauth.service;

import userauth.api.WalletApi;
import userauth.dao.WalletDAO;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;
import userauth.model.PaymentMethod;
import userauth.model.TopUpStatus;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;
import userauth.model.WalletTransaction;
import userauth.model.WalletTransactionType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class WalletService implements WalletApi {
    private static final long MIN_TOP_UP_AMOUNT = 10_000;
    private static final long MAX_TOP_UP_AMOUNT = 1_000_000_000;

    private final WalletDAO walletDAO;
    private final ConcurrentHashMap<Integer, ReentrantLock> walletLocks = new ConcurrentHashMap<>();

    public WalletService(WalletDAO walletDAO) {
        this.walletDAO = walletDAO;
    }

    public void initializeWalletForUser(int userId) throws ValidationException {
        try {
            withWalletLocks(() -> {
                Wallet existing = walletDAO.findWalletByUserId(userId);
                if (existing != null) {
                    throw new ValidationException("Wallet already exists for user ID: " + userId);
                }
                walletDAO.saveWallet(new Wallet(userId));
                return null;
            }, userId);
        } catch (ItemNotFoundException ex) {
            throw new IllegalStateException("Unexpected wallet lookup failure.", ex);
        }
    }

    @Override
    public Wallet getWallet(int userId) {
        try {
            return withWalletLocks(() -> getOrCreateWallet(userId), userId);
        } catch (ItemNotFoundException | ValidationException ex) {
            throw new IllegalStateException("Unable to read wallet.", ex);
        }
    }

     @Override
     public int createTopUpRequest(int userId, long amount, PaymentMethod method)
             throws ItemNotFoundException, ValidationException {
         validateTopUpAmount(amount);
         if (method == null) {
             throw new ValidationException("Payment method is required.");
         }
         return withWalletLocks(() -> {
             Wallet wallet = getOrCreateWallet(userId);

             TopUpTransaction transaction = new TopUpTransaction(userId, amount, method);
             int transactionId = walletDAO.saveTopUpTransaction(transaction);
             transaction.setStatus(TopUpStatus.SUCCESS);
             transaction.setReferenceCode("AUTO_CONFIRMED");
             transaction.setCompleteAt(System.currentTimeMillis());
             walletDAO.updateTopUpTransaction(transaction);

             long newBalance = wallet.getBalance() + amount;
             wallet.setBalance(newBalance);
             wallet.setUpdatedAt(System.currentTimeMillis());
             walletDAO.updateWallet(wallet);
             logWalletTransaction(userId, WalletTransactionType.TOP_UP, amount, null, "topup_tx:" + transactionId);

             return transactionId;
         }, userId);
     }

    @Override
    public List<TopUpTransaction> getTopUpHistory(int userId) throws ItemNotFoundException {
        try {
            return withWalletLocks(() -> {
                getOrCreateWallet(userId);
                return walletDAO.findTopUpTransactionsByUserId(userId);
            }, userId);
        } catch (ValidationException ex) {
            throw new IllegalStateException("Unexpected top-up history validation failure.", ex);
        }
    }

     public void ensureSufficientAvailableBalanceForBid(int userId, long targetBidAmount, long existingReservationCredit)
             throws ItemNotFoundException, ValidationException {
         validatePositiveAmount(targetBidAmount, "Bid amount");
         withWalletLocks(() -> {
             Wallet wallet = getOrCreateWallet(userId);
             long existingCredit = Math.max(existingReservationCredit, 0L);
             long usableBalance = wallet.getAvailableBalance() + existingCredit;

             if (usableBalance < targetBidAmount) {
                 throw new ValidationException(
                         "Insufficient available wallet balance. Available funds for this bid: " +
                                 formatMoney(usableBalance) + ". Required: " + formatMoney(targetBidAmount) + "."
                 );
             }
             return null;
         }, userId);
     }

     public void applyReservationTransition(int previousUserId, long previousAmount, int nextUserId, long nextAmount)
             throws ItemNotFoundException, ValidationException {
         int[] userIds = distinctPositiveIds(previousUserId, nextUserId);

         withWalletLocks(() -> {
             if (previousUserId > 0 && previousAmount > 0) {
                 if (previousUserId == nextUserId) {
                     long delta = nextAmount - previousAmount;
                     if (delta > 0) {
                         reserveAdditionalFunds(nextUserId, delta);
                     } else if (delta < 0) {
                         releaseReservedFundsInternal(nextUserId, -delta);
                     }
                     return null;
                 }
                 releaseReservedFundsInternal(previousUserId, previousAmount);
             }

             if (nextUserId > 0 && nextAmount > 0 && previousUserId != nextUserId) {
                 reserveAdditionalFunds(nextUserId, nextAmount);
             }
             return null;
         }, userIds);
     }

    public void captureReservedFunds(int userId, long amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Reserved amount");
        withWalletLocks(() -> {
            Wallet wallet = getOrCreateWallet(userId);
            if (wallet.getReservedBalance() < amount) {
                throw new ValidationException("Reserved wallet balance is lower than the amount to capture for user ID: " + userId);
            }
            wallet.setReservedBalance(Math.max(0L, wallet.getReservedBalance() - amount));
            wallet.setBalance(wallet.getBalance() - amount);
            wallet.setUpdatedAt(System.currentTimeMillis());
            walletDAO.updateWallet(wallet);
            logWalletTransaction(userId, WalletTransactionType.CAPTURE, amount, null, "auction_capture");
            return null;
        }, userId);
    }

    public void releaseReservedFunds(int userId, long amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Reserved amount");
        withWalletLocks(() -> {
            releaseReservedFundsInternal(userId, amount);
            return null;
        }, userId);
    }

    public void refundCapturedFunds(int userId, long amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Refund amount");
        withWalletLocks(() -> {
            Wallet wallet = getOrCreateWallet(userId);
            wallet.setBalance(wallet.getBalance() + amount);
            wallet.setUpdatedAt(System.currentTimeMillis());
            walletDAO.updateWallet(wallet);
            logWalletTransaction(userId, WalletTransactionType.REFUND, amount, null, "auction_refund");
            return null;
        }, userId);
    }

    public void reconcileReservedBalance(int userId, long expectedReservedBalance) throws ValidationException {
        if (expectedReservedBalance < 0) {
            throw new ValidationException("Expected reserved balance cannot be negative.");
        }
        try {
            withWalletLocks(() -> {
                Wallet wallet = getOrCreateWallet(userId);
                long targetReservedBalance = Math.min(expectedReservedBalance, wallet.getBalance());
                long currentReservedBalance = wallet.getReservedBalance();
                if (currentReservedBalance == targetReservedBalance) {
                    return null;
                }

                wallet.setReservedBalance(targetReservedBalance);
                wallet.setUpdatedAt(System.currentTimeMillis());
                walletDAO.updateWallet(wallet);

                long delta = Math.abs(targetReservedBalance - currentReservedBalance);
                WalletTransactionType type = targetReservedBalance > currentReservedBalance
                        ? WalletTransactionType.RESERVE
                        : WalletTransactionType.RELEASE;
                logWalletTransaction(userId, type, delta, null, "reservation_reconcile");

                return null;
            }, userId);
        } catch (ItemNotFoundException ex) {
            throw new IllegalStateException("Unexpected wallet lookup failure.", ex);
        }
    }

     private void reserveAdditionalFunds(int userId, long amount) throws ItemNotFoundException, ValidationException {
         validatePositiveAmount(amount, "Reserved amount");
         Wallet wallet = getOrCreateWallet(userId);
         long availableBalance = wallet.getAvailableBalance();

         if (availableBalance < amount) {
             throw new ValidationException("Insufficient wallet balance to reserve " + formatMoney(amount) +
                     ". Available: " + formatMoney(availableBalance) + ". Current balance: " +
                     formatMoney(wallet.getBalance()) + ", Reserved: " + formatMoney(wallet.getReservedBalance()) + ".");
         }
         wallet.setReservedBalance(wallet.getReservedBalance() + amount);
         wallet.setUpdatedAt(System.currentTimeMillis());
         walletDAO.updateWallet(wallet);
         logWalletTransaction(userId, WalletTransactionType.RESERVE, amount, null, "auction_reserve");

     }

     private void releaseReservedFundsInternal(int userId, long amount) throws ItemNotFoundException, ValidationException {
         Wallet wallet = getOrCreateWallet(userId);

         if (wallet.getReservedBalance() < amount) {
             throw new ValidationException("Reserved wallet balance is lower than the amount to release for user ID: " + userId);
         }
        wallet.setReservedBalance(Math.max(0L, wallet.getReservedBalance() - amount));
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
        logWalletTransaction(userId, WalletTransactionType.RELEASE, amount, null, "auction_release");

     }

    private Wallet getOrCreateWallet(int userId) {
        Wallet wallet = walletDAO.findWalletByUserId(userId);
        if (wallet != null) {
            return wallet;
        }
         try {
            Wallet created = new Wallet(userId);
            walletDAO.saveWallet(created);
            return created;
        } catch (IllegalStateException | ValidationException ex) {
            Wallet existing = walletDAO.findWalletByUserId(userId);
            if (existing != null) {
                return existing;
            }
            throw new IllegalStateException("Unable to create or retrieve wallet for user ID: " + userId, ex);
        }
    }

    private void validateTopUpAmount(long amount) throws ValidationException {
        if (amount < MIN_TOP_UP_AMOUNT) {
            throw new ValidationException("Top-up amount must be at least " + MIN_TOP_UP_AMOUNT + ".");
        }
        if (amount > MAX_TOP_UP_AMOUNT) {
            throw new ValidationException("Top-up amount must not exceed " + MAX_TOP_UP_AMOUNT + ".");
        }
        if (amount % 1000 != 0) {
            throw new ValidationException("Top-up amount must be a multiple of 1000 VND.");
        }
    }

    private void validatePositiveAmount(long amount, String label) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException(label + " must be greater than 0.");
        }
    }

    private String formatMoney(long amount) {
        return String.format("%,d VND", amount);
    }

    private void logWalletTransaction(int userId, WalletTransactionType type, long amount, Integer auctionId, String reference) {
        try {
            walletDAO.saveWalletTransaction(
                    new WalletTransaction(userId, type, amount, auctionId, reference)
            );
        } catch (Exception ex) {
            System.err.println("[Wallet] Failed to log transaction: " + ex.getMessage());
        }
    }

    private ReentrantLock getWalletLock(int userId) {
        return walletLocks.computeIfAbsent(userId, ignored -> new ReentrantLock());
    }

    @FunctionalInterface
    private interface LockedOperation<T> {
        T run() throws ItemNotFoundException, ValidationException;
    }

    private <T> T withWalletLocks(LockedOperation<T> operation, int... userIds)
            throws ItemNotFoundException, ValidationException {
        int[] sortedIds = Arrays.stream(userIds)
                .filter(id -> id > 0)
                .distinct()
                .sorted()
                .toArray();
        ReentrantLock[] locks = new ReentrantLock[sortedIds.length];
        for (int i = 0; i < sortedIds.length; i++) {
            locks[i] = getWalletLock(sortedIds[i]);
            locks[i].lock();
        }
        try {
            return operation.run();
        } finally {
            for (int i = locks.length - 1; i >= 0; i--) {
                locks[i].unlock();
            }
        }
    }

    private int[] distinctPositiveIds(int first, int second) {
        return Arrays.stream(new int[] {first, second})
                .filter(id -> id > 0)
                .distinct()
                .toArray();
    }
}
