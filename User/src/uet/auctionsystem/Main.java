package uet.auctionsystem;

import javafx.application.Application;
import javafx.stage.Stage;
import uet.auctionsystem.controller.AuctionController;
import uet.auctionsystem.controller.AuthController;
import uet.auctionsystem.controller.AutobidController;
import uet.auctionsystem.controller.WalletController;
import uet.auctionsystem.dao.AuctionDAO;
import uet.auctionsystem.dao.AuctionDAOImpl;
import uet.auctionsystem.dao.AutoBidDAO;
import uet.auctionsystem.dao.AutoBidDAOImpl;
import uet.auctionsystem.dao.UserDAO;
import uet.auctionsystem.dao.UserDAOImpl;
import uet.auctionsystem.dao.WalletDAO;
import uet.auctionsystem.dao.WalletDAOImpl;
import uet.auctionsystem.database.DatabaseInitializer;
import uet.auctionsystem.gui.fxml.AuthFrame;
import uet.auctionsystem.service.AuctionScheduler;
import uet.auctionsystem.service.AuctionService;
import uet.auctionsystem.service.AuthService;
import uet.auctionsystem.service.AutobidService;
import uet.auctionsystem.service.WalletService;

// Ghi chu file: Diem vao cua ung dung; khoi tao database, service, controller, scheduler va mo giao dien JavaFX.
// Khai bao lop Main; quan ly vong doi khoi dong, dung ung dung va mo giao dien chinh.
public class Main extends Application {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho property.
    private static final String SCHEDULER_PROPERTY = "app.scheduler.enabled";
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho env.
    private static final String SCHEDULER_ENV = "APP_SCHEDULER_ENABLED";
    // Thuoc tinh: giu tham chieu den AuctionScheduler de phoi hop xu ly.
    private AuctionScheduler scheduler;

    @Override
    // Phuong thuc: khoi dong hoac khoi tao tien trinh start.
    public void start(Stage stage) {
        DatabaseInitializer.initialize();

        UserDAO userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);
        AuthController authController = new AuthController(authService);

        AutoBidDAO autoBidDAO = new AutoBidDAOImpl();
        AutobidService autobidService = new AutobidService(autoBidDAO);
        AutobidController autobidController = new AutobidController(autobidService);

        WalletDAO walletDAO = new WalletDAOImpl();
        WalletService walletService = new WalletService(walletDAO);
        WalletController walletController = new WalletController(walletService);

        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AuctionService auctionService = new AuctionService(auctionDAO, walletService);
        AuctionController auctionController = new AuctionController(auctionService);


        if (isSchedulerEnabled()) {
            scheduler = new AuctionScheduler(auctionService);
            scheduler.start();
        }

        AuthFrame frame = new AuthFrame(
                stage,
                authController,
                auctionController,
                autobidController,
                walletController
        );
        frame.show();
        frame.showHome();
    }

    @Override
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac stop.
    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }
    // Ham tao: khoi tao doi tuong Main voi cac phu thuoc can thiet.
    public static void main(String[] args) {
        launch(args);
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac is scheduler enabled.
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
