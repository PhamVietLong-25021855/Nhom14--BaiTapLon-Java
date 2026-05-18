package userauth.controller;

import userauth.api.WalletApi;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;
import userauth.model.PaymentMethod;
import userauth.model.TopUpTransaction;
import userauth.model.Wallet;

import java.util.List;

public class WalletController {
    private final WalletApi walletService;

    public WalletController(WalletApi walletService) {
        this.walletService = walletService;
    }

    public Wallet getWallet(int userId) {
        try {
            return walletService.getWallet(userId);
        } catch (ItemNotFoundException ex) {
            return null;
        }
    }

    public String createTopUpRequest(int userId, double amount, PaymentMethod method) {
        try {
            int transactionId = walletService.createTopUpRequest(userId, amount, method);
            return "SUCCESS: Transaction ID " + transactionId;
        } catch (ItemNotFoundException | ValidationException ex) {
            return ex.getMessage();
        }
    }

    public List<TopUpTransaction> getTopUpHistory(int userId) {
        try {
            return walletService.getTopUpHistory(userId);
        } catch (ItemNotFoundException ex) {
            return List.of();
        }
    }
}
