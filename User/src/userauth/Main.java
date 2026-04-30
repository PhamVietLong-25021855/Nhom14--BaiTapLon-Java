package userauth;

import javafx.application.Application;
import javafx.stage.Stage;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.HomepageController;
import userauth.dao.AuctionDAO;
import userauth.dao.AuctionDAOImpl;
import userauth.dao.UserDAO;
import userauth.dao.UserDAOImpl;
import userauth.database.DatabaseInitializer;
import userauth.gui.fxml.AuthFrame;
import userauth.service.AuctionScheduler;
import userauth.service.AuctionService;
import userauth.service.AuthService;
import userauth.service.HomepageContentService;

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
        HomepageContentService homepageContentService = new HomepageContentService();
        HomepageController homepageController = new HomepageController(homepageContentService);

        if (isSchedulerEnabled()) {
            scheduler = new AuctionScheduler(auctionService);
            scheduler.start();
        }

        AuthFrame frame = new AuthFrame(stage, authController, auctionController, homepageController);
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
