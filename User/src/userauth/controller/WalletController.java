package userauth.controller;

import userauth.client.network.RemoteApiClient;
import userauth.common.RemoteAction;
import userauth.common.RemoteResponse;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;
import userauth.model.PaymentMethod;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;
import userauth.service.WalletService;

import java.util.List;

public class WalletController extends RemoteControllerSupport {
    private final WalletService walletService;
    private final RemoteApiClient remoteApiClient;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
        this.remoteApiClient = null;
    }

    public WalletController(RemoteApiClient remoteApiClient) {
        this.walletService = null;
        this.remoteApiClient = remoteApiClient;
    }

    public String initializeWallet(int userId) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.WALLET_INITIALIZE, userId);
        }

        try {
            walletService.initializeWalletForUser(userId);
            return "SUCCESS";
        } catch (ValidationException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String createTopUpRequest(int userId, double amount, PaymentMethod method) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.WALLET_CREATE_TOPUP, userId, amount, method);
        }

        try {
            int transactionId = walletService.createTopUpRequest(userId, amount, method);
            return "SUCCESS: Transaction ID " + transactionId;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String confirmTopUp(int transactionId, String reference) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.WALLET_CONFIRM_TOPUP, transactionId, reference);
        }

        try {
            walletService.confirmTopUp(transactionId, reference);
            return "SUCCESS";
        } catch (ValidationException | ItemNotFoundException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String cancelTopUp(int transactionId) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.WALLET_CANCEL_TOPUP, transactionId);
        }

        try {
            walletService.cancelTopUp(transactionId);
            return "SUCCESS";
        } catch (ValidationException | ItemNotFoundException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public Wallet getWallet(int userId) {
        if (remoteApiClient != null) {
            return requestPayload(remoteApiClient, Wallet.class, RemoteAction.WALLET_GET_WALLET, userId);
        }

        try {
            return walletService.getWallet(userId);
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    public String deductFromWallet(int userId, double amount) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.WALLET_DEDUCT, userId, amount);
        }

        try {
            walletService.deductFromWallet(userId, amount);
            return "SUCCESS";
        } catch (ValidationException | ItemNotFoundException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String addToWallet(int userId, double amount) {
        if (remoteApiClient != null) {
            return requestString(RemoteAction.WALLET_ADD, userId, amount);
        }

        try {
            walletService.addToWallet(userId, amount);
            return "SUCCESS";
        } catch (ValidationException | ItemNotFoundException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    public List<TopUpTransaction> getTopUpHistory(int userId) {
        if (remoteApiClient != null) {
            return requestList(remoteApiClient, RemoteAction.WALLET_GET_TOPUP_HISTORY, userId);
        }

        try {
            return walletService.getTopUpHistory(userId);
        } catch (ItemNotFoundException e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public List<TopUpTransaction> getAllPendingTrasactions() {
        if (remoteApiClient != null) {
            return requestList(remoteApiClient, RemoteAction.WALLET_GET_ALL_PENDING);
        }

        return walletService.getAllPendingTransactions();
    }

    public double getWalletBalance(int userId) {
        if (remoteApiClient != null) {
            return requestDouble(remoteApiClient, RemoteAction.WALLET_GET_BALANCE, userId);
        }

        Wallet wallet = getWallet(userId);
        return wallet != null ? wallet.getBalance() : 0.0;
    }

    private String requestString(RemoteAction action, Object... arguments) {
        String result = requestString(remoteApiClient, action, arguments);
        return result == null ? "ERROR: Request failed." : result;
    }
}
