package userauth.server;

import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.HomepageController;
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
    private final AuctionScheduler scheduler;

    public ServerContext(boolean startScheduler) {
        DatabaseInitializer.initialize();

        UserDAO userDAO = new UserDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AutoBidDAO autoBidDAO = new AutoBidDAOImpl();

        AutoBidInitializer autoBidInitializer = new AutoBidInitializer(autoBidDAO, auctionDAO);
        AuthService authService = new AuthService(userDAO, autoBidInitializer);
        this.authController = new AuthController(authService);

        this.auctionService = new AuctionService(auctionDAO, autoBidDAO);
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

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public AutobidController getAutobidController() {
        return autobidController;
    }

    public HomepageController getHomepageController() {
        return homepageController;
    }

    public HomepageContentService getHomepageContentService() {
        return homepageContentService;
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }
}
