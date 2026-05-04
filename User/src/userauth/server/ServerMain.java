package userauth.server;

import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.HomepageController;
import userauth.controller.WalletController;
import userauth.dao.AuctionDAO;
import userauth.dao.AuctionDAOImpl;
import userauth.dao.AutoBidDAO;
import userauth.dao.AutoBidDAOImpl;
import userauth.dao.UserDAO;
import userauth.dao.UserDAOImpl;
import userauth.dao.WalletDAO;
import userauth.dao.WalletDAOImpl;
import userauth.database.DatabaseInitializer;
import userauth.service.AuctionScheduler;
import userauth.service.AuctionService;
import userauth.service.AuthService;
import userauth.service.AutobidService;
import userauth.service.HomepageContentService;
import userauth.service.WalletService;

public final class ServerMain {
    private static final String PORT_PROPERTY = "app.server.port";
    private static final String PORT_ENV = "APP_SERVER_PORT";
    private static final int DEFAULT_PORT = 9999;
    private static final String SCHEDULER_PROPERTY = "app.scheduler.enabled";
    private static final String SCHEDULER_ENV = "APP_SCHEDULER_ENABLED";
    private static final String DB_INIT_PROPERTY = "app.db.init.enabled";
    private static final String DB_INIT_ENV = "APP_DB_INIT_ENABLED";

    private ServerMain() {
    }

    public static void main(String[] args) {
        if (isDatabaseInitializationEnabled()) {
            DatabaseInitializer.initialize();
        }

        UserDAO userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);
        AuthController authController = new AuthController(authService);

        WalletDAO walletDAO = new WalletDAOImpl();
        WalletService walletService = new WalletService(walletDAO);
        WalletController walletController = new WalletController(walletService);

        AutoBidDAO autoBidDAO = new AutoBidDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AutobidService autobidService = new AutobidService(autoBidDAO, auctionDAO);
        AutobidController autobidController = new AutobidController(autobidService);
        AuctionService auctionService = new AuctionService(auctionDAO, autoBidDAO, walletService);
        AuctionController auctionController = new AuctionController(auctionService);

        HomepageContentService homepageContentService = new HomepageContentService();
        HomepageController homepageController = new HomepageController(homepageContentService);

        if (isSchedulerEnabled()) {
            AuctionScheduler scheduler = new AuctionScheduler(auctionService);
            scheduler.start();
        }

        ServerRequestDispatcher dispatcher = new ServerRequestDispatcher(
                authController,
                auctionController,
                autobidController,
                homepageController,
                walletController
        );
        new SocketAuctionServer(resolvePort(), dispatcher).start();
    }

    private static int resolvePort() {
        String propertyValue = System.getProperty(PORT_PROPERTY);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Integer.parseInt(propertyValue.trim());
        }

        String envValue = System.getenv(PORT_ENV);
        if (envValue != null && !envValue.isBlank()) {
            return Integer.parseInt(envValue.trim());
        }

        return DEFAULT_PORT;
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

    private static boolean isDatabaseInitializationEnabled() {
        String propertyValue = System.getProperty(DB_INIT_PROPERTY);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Boolean.parseBoolean(propertyValue.trim());
        }

        String envValue = System.getenv(DB_INIT_ENV);
        if (envValue != null && !envValue.isBlank()) {
            return Boolean.parseBoolean(envValue.trim());
        }

        return true;
    }
}
