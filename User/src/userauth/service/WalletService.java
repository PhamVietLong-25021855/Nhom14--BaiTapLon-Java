package userauth.service;

import userauth.dao.WalletDAO;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;
import userauth.model.PaymentMethod;
import userauth.model.TopUpStatus;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;
import java.util.List;

// Ghi chu file: File service; chua nghiep vu chinh va phoi hop giua controller, DAO va event.
// Khai bao lop WalletService; chua xu ly nghiep vu va cac quy tac chinh cua he thong.
public class WalletService {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho amount.
    private static final double MIN_TOPUP_AMOUNT = 10_000;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho amount.
    private static final double MAX_TOPUP_AMOUNT = 100_000_000;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho epsilon.
    private static final double BALANCE_EPSILON = 0.0001;
    // Thuoc tinh: giu tham chieu den WalletDAO de phoi hop xu ly.
    private final WalletDAO walletDAO;
    // Ham tao: khoi tao doi tuong WalletService voi cac phu thuoc can thiet.
    public WalletService(WalletDAO walletDAO) {
        this.walletDAO = walletDAO;
    }
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize wallet for user.
    public void initializeWalletForUser(int userId) throws ValidationException {
        if (walletDAO.findWalletByUserId(userId) != null) {
            throw new ValidationException("Wallet already exists for user ID: " + userId);
        }
        walletDAO.saveWallet(new Wallet(userId));
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get wallet.
    public Wallet getWallet(int userId) throws ItemNotFoundException {
        Wallet wallet = walletDAO.findWalletByUserId(userId);
        if (wallet == null) {
            throw new ItemNotFoundException("Wallet not found for user ID: " + userId);
        }
        return wallet;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create top up request.
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
    // Phuong thuc: thuc hien chuc nang confirm top up trong lop WalletService.
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
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac cancel top up.
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
    // Phuong thuc: thuc hien chuc nang deduct from wallet trong lop WalletService.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add to wallet.
    public void addToWallet(int userId, double amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Amount");
        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac ensure sufficient available balance for bid.
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
    // Phuong thuc: thuc hien chuc nang release reserved funds trong lop WalletService.
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
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac capture reserved funds.
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

    // HoÃ n tiá»n láº¡i vÃ o sá»‘ dÆ° thá»±c khi seller há»§y káº¿t quáº£ cá»§a auction Ä‘Ã£ PAID.
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac refund captured funds.
    public void refundCapturedFunds(int userId, double amount) throws ItemNotFoundException, ValidationException {
        validatePositiveAmount(amount, "Refund amount");

        Wallet wallet = getWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletDAO.updateWallet(wallet);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get top up history.
    public List<TopUpTransaction> getTopUpHistory(int userId) throws ItemNotFoundException {
        getWallet(userId);
        return walletDAO.findTopUpTransactionsByUserId(userId);
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get all pending transactions.
    public List<TopUpTransaction> getAllPendingTransactions() {
        return walletDAO.findAllPendingTransactions();
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac ensure wallet exists.
    private void ensureWalletExists(int userId) {
        if (walletDAO.findWalletByUserId(userId) != null) {
            return;
        }
        walletDAO.saveWallet(new Wallet(userId));
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac validate top up amount.
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
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac validate positive amount.
    private void validatePositiveAmount(double amount, String label) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException(label + " must be greater than 0.");
        }
    }
    // Phuong thuc: bien doi du lieu cho thao tac format money.
    private String formatMoney(double amount) {
        return String.format("%,.0f VND", amount);
    }
}
