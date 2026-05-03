package userauth.controller;

import userauth.model.Wallet;
import userauth.model.PaymentMethod;
import userauth.model.TopUpTransaction;
import userauth.service.WalletService;
import userauth.exception.ItemNotFoundException;
import userauth.exception.ValidationException;

import java.util.List;

public class WalletController{
    private final WalletService walletService;

    public WalletController(WalletService walletService){this.walletService = walletService;}

    public String initializeWallet(int userId){
        try{
            walletService.initializeWalletForUser(userId);
            return "SUCCESS";
        } catch (ValidationException e){
            return "ERROR: " + e.getMessage();
        }
    }

    public String createTopUpRequest(int userId, double amount, PaymentMethod method){
        try{
            int transactionId = walletService.createTopUpRequest(userId, amount, method);
            return "SUCCESS: Transaction ID " + transactionId;
        } catch(Exception e){
            return "ERROR: " + e.getMessage();
        }
    }

    public String confirmTopUp(int transactionId, String reference){
        try{
            walletService.confirmTopUp(transactionId,reference);
            return "SUCCESS";
        }catch(ValidationException | ItemNotFoundException e){
            return "ERROR: " + e.getMessage();
        }
    }

    public String cancelTopUp(int transactionId){
        try{
            walletService.cancelTopUp(transactionId);
            return "SUCCESS";
        }catch(ValidationException | ItemNotFoundException e){
            return "ERROR: " + e.getMessage();
        }
    }

    public Wallet getWallet(int userId){
        try{
            return walletService.getWallet(userId);
        }catch (ItemNotFoundException e){
            return null;
        }
    }

    public String deductFromWallet(int userId, double amount){
        try{
            walletService.deductFromWallet(userId, amount);
            return "SUCCESS";
        }catch(ValidationException | ItemNotFoundException e){
            return "ERROR: " + e.getMessage();
        }
    }

    public String addToWallet(int userId, double amount){
        try{
            walletService.addToWallet(userId, amount);
            return "SUCCESS";
        }catch(ValidationException | ItemNotFoundException e){
            return "ERROR: " + e.getMessage();
        }
    }

    public List<TopUpTransaction> getTopUpHistory(int userId){
        try{
            return walletService.getTopUpHistory(userId);
        }catch (ItemNotFoundException e){
            return List.of();
        }
    }

    public List<TopUpTransaction> getAllPendingTrasactions(){
        return walletService.getAllPendingTransactions();
    }

    public double getWalletBalance(int userId){
        Wallet wallet = getWallet(userId);
        return wallet != null ? wallet.getBalance() : 0.0;
    }
}
