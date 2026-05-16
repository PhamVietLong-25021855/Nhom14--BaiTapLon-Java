package userauth.server;

import userauth.database.DatabaseInitializer;
import userauth.dao.AuctionDAO;
import userauth.dao.AuctionDAOImpl;
import userauth.dao.AutoBidDAO;
import userauth.dao.AutoBidDAOImpl;
import userauth.dao.HomepageAnnouncementDAO;
import userauth.dao.HomepageAnnouncementDAOImpl;
import userauth.dao.UserDAO;
import userauth.dao.UserDAOImpl;
import userauth.service.AutoBidInitializer;
import userauth.service.AuctionScheduler;
import userauth.service.AuctionService;
import userauth.service.AuthService;
import userauth.service.HomepageContentService;
import userauth.service.AutobidService;

public final class AuctionServerMain {
    private static final int DEFAULT_PORT = 5050;
    private static final String SCHEDULER_PROPERTY = "app.scheduler.enabled";
    private static final String SCHEDULER_ENV = "APP_SCHEDULER_ENABLED";

    private final int port;
    private final AuctionScheduler scheduler;
    private AuctionSocketServer socketServer;

    public AuctionServerMain() {
        this(DEFAULT_PORT);
    }

    public AuctionServerMain(int port) {
        this.port = port;
        DatabaseInitializer.initialize();

        UserDAO userDAO = new UserDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AutoBidDAO autoBidDAO = new AutoBidDAOImpl();
        HomepageAnnouncementDAO homepageAnnouncementDAO = new HomepageAnnouncementDAOImpl();

        AutoBidInitializer autoBidInitializer = new AutoBidInitializer(autoBidDAO, auctionDAO);
        AuthService authService = new AuthService(userDAO, autoBidInitializer);
        AuctionService auctionService = new AuctionService(auctionDAO, autoBidDAO);
        HomepageContentService homepageContentService = new HomepageContentService(homepageAnnouncementDAO);

        ServerContext context = new ServerContext(authService, auctionService,
                new AutobidService(autoBidDAO, auctionService), homepageContentService);

        AuctionRequestHandler handler = new AuctionRequestHandler(context);
        this.socketServer = new AuctionSocketServer(port, handler);

        if (isSchedulerEnabled()) {
            this.scheduler = new AuctionScheduler(auctionService);
        } else {
            this.scheduler = null;
        }
    }

    public void start() {
        if (scheduler != null) {
            scheduler.start();
            System.out.println("[AuctionServer] AuctionScheduler started.");
        }
        try (AuctionSocketServer server = socketServer) {
            server.start();
        } catch (Exception ex) {
            System.err.println("[AuctionServer] Server error: " + ex.getMessage());
            ex.printStackTrace();
        }
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

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                System.err.println("Invalid port: " + args[0] + ", using default " + DEFAULT_PORT);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[AuctionServer] Shutdown signal received.");
        }, "main-shutdown"));
        new AuctionServerMain(port).start();
    }
}
