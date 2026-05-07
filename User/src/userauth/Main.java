package userauth;

import javafx.application.Application;
import javafx.stage.Stage;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.HomepageController;
import userauth.client.remote.*;
import userauth.dao.*;
import userauth.database.DatabaseInitializer;
import userauth.gui.fxml.shell.AuthFrame;
import userauth.service.*;

public class Main extends Application {
    private static final String SCHEDULER_PROPERTY = "app.scheduler.enabled";
    private static final String SCHEDULER_ENV = "APP_SCHEDULER_ENABLED";

    private AuctionScheduler scheduler;

    @Override
    public void start(Stage stage) {
        boolean remoteMode = isRemoteClientMode();

        AuthService authService;
        AuctionService auctionService;
        AutobidService autobidService;
        HomepageContentService homepageContentService;

        if (remoteMode) {
            RemoteAuctionClient remoteClient = new RemoteAuctionClient();
            authService = new RemoteAuthService(remoteClient);
            auctionService = new RemoteAuctionService(remoteClient);
            autobidService = new RemoteAutobidService(remoteClient);
            homepageContentService = new RemoteHomepageContentService(remoteClient);
            System.out.println("[Client] Remote mode: using server " + RemoteClientConfig.host() + ":" + RemoteClientConfig.port());
        } else {
            DatabaseInitializer.initialize();

            UserDAO userDAO = new UserDAOImpl();
            authService = new AuthService(userDAO);

            AuctionDAO auctionDAO = new AuctionDAOImpl();
            AutoBidDAO autoBidDAO = new AutoBidDAOImpl();
            auctionService = new AuctionService(auctionDAO, autoBidDAO);
            autobidService = new AutobidService(autoBidDAO);
            homepageContentService = new HomepageContentService();

            if (isSchedulerEnabled()) {
                scheduler = new AuctionScheduler(auctionService);
                scheduler.start();
            }
        }

        AuthController authController = new AuthController(authService);
        AuctionController auctionController = new AuctionController(auctionService);
        AutobidController autobidController = new AutobidController(autobidService);
        HomepageController homepageController = new HomepageController(homepageContentService);

        AuthFrame frame = new AuthFrame(stage, authController, auctionController, homepageController, autobidController);
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

    private static boolean isRemoteClientMode() {
        String propertyValue = System.getProperty("app.client.mode");
        if (propertyValue != null && !propertyValue.isBlank()) {
            return !"local".equalsIgnoreCase(propertyValue.trim());
        }
        String envValue = System.getenv("APP_CLIENT_MODE");
        if (envValue != null && !envValue.isBlank()) {
            return !"local".equalsIgnoreCase(envValue.trim());
        }
        return true;
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
