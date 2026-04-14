package userauth;

import javafx.application.Application;
import javafx.stage.Stage;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.dao.AuctionDAO;
import userauth.dao.AuctionDAOImpl;
import userauth.dao.UserDAO;
import userauth.dao.UserDAOImpl;
import userauth.gui.AuthFrame;
import userauth.service.AuctionScheduler;
import userauth.service.AuctionService;
import userauth.service.AuthService;

public class Main extends Application {
    private AuctionScheduler scheduler;

    @Override
    public void start(Stage stage) {
        UserDAO userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);
        AuthController authController = new AuthController(authService);

        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AuctionService auctionService = new AuctionService(auctionDAO);
        AuctionController auctionController = new AuctionController(auctionService);

        scheduler = new AuctionScheduler(auctionService);
        scheduler.start();

        AuthFrame frame = new AuthFrame(stage, authController, auctionController);
        frame.showLogin();
        frame.show();
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
