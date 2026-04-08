package userauth;

import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.dao.AuctionDAO;
import userauth.dao.AuctionDAOImpl;
import userauth.dao.UserDAO;
import userauth.dao.UserDAOImpl;
import userauth.gui.AuthFrame;
import userauth.service.AuctionService;
import userauth.service.AuthService;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        UserDAO userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);
        AuthController authController = new AuthController(authService);
        
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AuctionService auctionService = new AuctionService(auctionDAO);
        AuctionController auctionController = new AuctionController(auctionService);

        userauth.service.AuctionScheduler scheduler = new userauth.service.AuctionScheduler(auctionService);
        scheduler.start();

        // Chạy Java Swing trên Event Dispatch Thread (Best Practice)
        SwingUtilities.invokeLater(() -> {
            AuthFrame frame = new AuthFrame(authController, auctionController);
            frame.setVisible(true);
            frame.showLogin(); 
        });
    }
}