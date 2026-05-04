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

public class WalletController {
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
            try {
                RemoteResponse response = remoteApiClient.send(RemoteAction.WALLET_GET_WALLET, userId);
                return response.isSuccess() ? response.payloadAs(Wallet.class) : null;
            } catch (RuntimeException ex) {
                return null;
            }
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
            try {
                RemoteResponse response = remoteApiClient.send(RemoteAction.WALLET_GET_TOPUP_HISTORY, userId);
                if (!response.isSuccess()) {
                    return List.of();
                }
                Object payload = response.getPayload();
                return payload instanceof List<?> items ? (List<TopUpTransaction>) items : List.of();
            } catch (RuntimeException ex) {
                return List.of();
            }
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
            try {
                RemoteResponse response = remoteApiClient.send(RemoteAction.WALLET_GET_ALL_PENDING);
                if (!response.isSuccess()) {
                    return List.of();
                }
                Object payload = response.getPayload();
                return payload instanceof List<?> items ? (List<TopUpTransaction>) items : List.of();
            } catch (RuntimeException ex) {
                return List.of();
            }
        }

        return walletService.getAllPendingTransactions();
    }

    public double getWalletBalance(int userId) {
        if (remoteApiClient != null) {
            try {
                RemoteResponse response = remoteApiClient.send(RemoteAction.WALLET_GET_BALANCE, userId);
                if (!response.isSuccess()) {
                    return 0.0;
                }
                Object payload = response.getPayload();
                return payload instanceof Double value ? value : 0.0;
            } catch (RuntimeException ex) {
                return 0.0;
            }
        }

        Wallet wallet = getWallet(userId);
        return wallet != null ? wallet.getBalance() : 0.0;
    }

    private String requestString(RemoteAction action, Object... arguments) {
        try {
            RemoteResponse response = remoteApiClient.send(action, arguments);
            if (!response.isSuccess()) {
                return response.getMessage();
            }
            String payload = response.payloadAsString();
            return payload == null || payload.isBlank() ? "SUCCESS" : payload;
        } catch (RuntimeException ex) {
            return "ERROR: " + ex.getMessage();
        }
    }
}
