package userauth.server;

import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.HomepageController;
import userauth.service.AuctionService;
import userauth.service.AuthService;
import userauth.service.HomepageContentService;
import userauth.service.AutobidService;

/**
 * Holds all server-side controllers for request handling.
 */
public final class ServerContext {
    private final AuthController authController;
    private final AuctionController auctionController;
    private final AuctionService auctionService;
    private final AutobidController autobidController;
    private final HomepageController homepageController;
    private final HomepageContentService homepageContentService;

    public ServerContext(AuthService authService, AuctionService auctionService,
                         AutobidService autobidService, HomepageContentService homepageContentService) {
        this.authController = new AuthController(authService);
        this.auctionController = new AuctionController(auctionService);
        this.auctionService = auctionService;
        this.autobidController = new AutobidController(autobidService);
        this.homepageContentService = homepageContentService;
        this.homepageController = new HomepageController(homepageContentService);
    }

    public AuthController getAuthController() { return authController; }
    public AuctionController getAuctionController() { return auctionController; }
    public AuctionService getAuctionService() { return auctionService; }
    public AutobidController getAutobidController() { return autobidController; }
    public HomepageController getHomepageController() { return homepageController; }
    public HomepageContentService getHomepageContentService() { return homepageContentService; }
}
