package userauth.server;

import userauth.controller.*;
import userauth.dao.*;
import userauth.database.DatabaseInitializer;
import userauth.service.*;

/**
 * Khởi tạo toàn bộ dependency của phía Server.
 * Theo kiến trúc Client-Server, chỉ lớp chạy trong server mới tạo DAO và truy cập database.
 */
public final class ServerContext {
    private final AuthController authController;
    private final AuctionController auctionController;
    private final AuctionService auctionService;
    private final AutobidController autobidController;
    private final HomepageController homepageController;
    private final HomepageContentService homepageContentService;
    private final WalletController walletController;
    private final WalletService walletService;
    private final AuctionScheduler scheduler;
    private final NotificationController notificationController;

    public ServerContext(boolean startScheduler) {
        DatabaseInitializer.initialize();

        UserDAO userDAO = new UserDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AutoBidDAO autoBidDAO = new AutoBidDAOImpl();
        WalletDAO walletDAO = new WalletDAOImpl();
        NotificationDAO notificationDAO = new NotificationDAOImpl();

        this.walletService = new WalletService(walletDAO);
        AuthService authService = new AuthService(userDAO, walletService);
        this.authController = new AuthController(authService);
        this.walletController = new WalletController(walletService);

        NotificationService notificationService = new NotificationService(notificationDAO);
        this.notificationController = new NotificationController(notificationService);

        this.auctionService = new AuctionService(auctionDAO, autoBidDAO, walletService, notificationService);
        this.auctionService.reconcileReservedBalances();
        this.auctionController = new AuctionController(this.auctionService);

        AutobidService autobidService = new AutobidService(autoBidDAO, this.auctionService);
        this.autobidController = new AutobidController(autobidService);

        this.homepageContentService = new HomepageContentService();
        this.homepageController = new HomepageController(homepageContentService);



        this.scheduler = startScheduler ? new AuctionScheduler(this.auctionService) : null;
        if (this.scheduler != null) {
            this.scheduler.start();
        }
    }

    public AuthController getAuthController() {
        return authController;
    }

    public AuctionController getAuctionController() {
        return auctionController;
    }

    public AuctionService getAuctionService() {return auctionService;}

    public AutobidController getAutobidController() {
        return autobidController;
    }

    public HomepageController getHomepageController() {
        return homepageController;
    }

    public HomepageContentService getHomepageContentService() {
        return homepageContentService;
    }

    public WalletController getWalletController() {return walletController;}

    public WalletService getWalletService() {return walletService;}

    public NotificationController getNotificationController () {return notificationController;}

    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }
}
