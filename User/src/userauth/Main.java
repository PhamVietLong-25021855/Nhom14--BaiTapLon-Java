package userauth;

import javafx.application.Application;
import javafx.stage.Stage;
import userauth.controller.*;
import userauth.dao.*;
import userauth.database.DatabaseInitializer;
import userauth.gui.fxml.AuthFrame;
import userauth.service.*;

public class Main extends Application {
    private static final String SCHEDULER_PROPERTY = "app.scheduler.enabled";
    private static final String SCHEDULER_ENV = "APP_SCHEDULER_ENABLED";

    private AuctionScheduler scheduler;

    @Override
    public void start(Stage stage) {
        DatabaseInitializer.initialize();

        UserDAO userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);
        AuthController authController = new AuthController(authService);

        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AuctionService auctionService = new AuctionService(auctionDAO);
        AuctionController auctionController = new AuctionController(auctionService);


        AutoBidDAO autoBidDAO = new AutoBidDAOImpl();
        AutobidService autobidService = new AutobidService(autoBidDAO);
        AutobidController autobidController = new AutobidController(autobidService);
        HomepageContentService homepageContentService = new HomepageContentService();
        HomepageController homepageController = new HomepageController(homepageContentService);

        WalletDAO walletDAO = new WalletDAOImpl();
        WalletService walletService = new WalletService(walletDAO);
        WalletController walletController = new WalletController(walletService);

        if (isSchedulerEnabled()) {
            scheduler = new AuctionScheduler(auctionService);
            scheduler.start();
        }

        AuthFrame frame = new AuthFrame(stage, authController, auctionController, homepageController, autobidController, walletController);
        frame.show();
        frame.showHome();
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

    private static boolean isSchedulerEnabled() {
        String propertyValue = System.getProperty(SCHEDULER_PROPERTY);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Boolean.parseBoolean(propertyValue.trim());
        }

        String envValue = System.getenv(SCHEDULER_ENV);
        if (envValue != null && !envValue.isBlank()) {
            return Boolean.parseBoolean(envValue.trim());
        }

        return true;
    }
}
