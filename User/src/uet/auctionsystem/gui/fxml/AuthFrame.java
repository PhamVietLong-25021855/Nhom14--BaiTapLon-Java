package uet.auctionsystem.gui.fxml;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import uet.auctionsystem.controller.AuctionController;
import uet.auctionsystem.controller.AuthController;
import uet.auctionsystem.controller.AutobidController;
import uet.auctionsystem.controller.HomepageController;
import uet.auctionsystem.controller.WalletController;
import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.BidTransaction;
import uet.auctionsystem.model.User;
import java.util.List;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop AuthFrame; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class AuthFrame {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho width.
    private static final double DEFAULT_WIDTH = 1280;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho height.
    private static final double DEFAULT_HEIGHT = 840;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho fullscreen.
    private static final boolean OPEN_FULLSCREEN = true;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final Stage stage;
    // Thuoc tinh: giu tham chieu den AuthController de phoi hop xu ly.
    private final AuthController authController;
    // Thuoc tinh: giu tham chieu den AuctionController de phoi hop xu ly.
    private final AuctionController auctionController;
    // Thuoc tinh: giu tham chieu den AutobidController de phoi hop xu ly.
    private final AutobidController autobidController;
    // Thuoc tinh: giu tham chieu den WalletController de phoi hop xu ly.
    private final WalletController walletController;
    // Thuoc tinh: giu tham chieu den HomepageController de phoi hop xu ly.
    private final HomepageController homepageController;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final Scene scene;
    // Thuoc tinh: giu tham chieu den AppShellController de phoi hop xu ly.
    private final AppShellController shellController;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final LoadedView<HomeViewController> homeView;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final LoadedView<LoginViewController> loginView;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final LoadedView<RegisterViewController> registerView;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final LoadedView<AdminDashboardViewController> adminView;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final LoadedView<AdminHomepageViewController> adminHomepageView;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final LoadedView<SellerDashboardViewController> sellerView;
    // Thuoc tinh: giu phu thuoc can dung xuyen suot vong doi doi tuong.
    private final LoadedView<BidderDashboardViewController> bidderView;
    // Ham tao: khoi tao doi tuong AuthFrame voi cac phu thuoc can thiet.
    public AuthFrame(
            Stage stage,
            AuthController authController,
            AuctionController auctionController,
            HomepageController homepageController,
            AutobidController autobidController,
            WalletController walletController
    ) {
        this.stage = stage;
        this.authController = authController;
        this.auctionController = auctionController;
        this.autobidController = autobidController;
        this.walletController = walletController;
        this.homepageController = homepageController;

        stage.setTitle(UiText.text("PRODUCT AUCTION PLATFORM"));
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.setMaximized(OPEN_FULLSCREEN);
        if (OPEN_FULLSCREEN) {
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
        }

        LoadedView<AppShellController> shellView = FxmlRuntime.loadView(AuthFrame.class, "app-shell.fxml", "view");
        this.shellController = shellView.controller();

        homeView = FxmlRuntime.loadView(AuthFrame.class, "home-view.fxml", "view");
        loginView = FxmlRuntime.loadView(AuthFrame.class, "login-view.fxml", "view");
        registerView = FxmlRuntime.loadView(AuthFrame.class, "register-view.fxml", "view");
        adminView = FxmlRuntime.loadView(AuthFrame.class, "admin-dashboard-view.fxml", "view");
        adminHomepageView = FxmlRuntime.loadView(AuthFrame.class, "admin-homepage-view.fxml", "view");
        sellerView = FxmlRuntime.loadView(AuthFrame.class, "seller-dashboard-view.fxml", "view");
        bidderView = FxmlRuntime.loadView(AuthFrame.class, "bidder-dashboard-view.fxml", "view");

        wireControllers();

        shellController.setContent(homeView.root());
        scene = new Scene(shellView.root(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        stage.setScene(scene);
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show.
    public void show() {
        stage.show();
        if (!OPEN_FULLSCREEN) {
            stage.centerOnScreen();
        }
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get window.
    public Window getWindow() {
        return stage;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set language.
    public void setLanguage(AppLanguage language) {
        UiText.setCurrentLanguage(language);
        stage.setTitle(UiText.text("PRODUCT AUCTION PLATFORM"));
        applyLanguage(homeView.root());
        applyLanguage(loginView.root());
        applyLanguage(registerView.root());
        applyLanguage(adminView.root());
        applyLanguage(adminHomepageView.root());
        applyLanguage(sellerView.root());
        applyLanguage(bidderView.root());
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show home.
    public void showHome() {
        deactivateLiveViews();
        switchView(homeView.root());
        homeView.controller().activate();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show login.
    public void showLogin() {
        deactivateLiveViews();
        switchView(loginView.root());
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show register.
    public void showRegister() {
        deactivateLiveViews();
        switchView(registerView.root());
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show role dashboard.
    public void showRoleDashboard(User user) {
        switch (user.getRole()) {
            case ADMIN -> showAdminDashboard(user);
            case SELLER -> {
                deactivateLiveViews();
                sellerView.controller().setUser(user);
                switchView(sellerView.root());
                sellerView.controller().activate();
            }
            case BIDDER -> {
                deactivateLiveViews();
                bidderView.controller().setUser(user);
                switchView(bidderView.root());
                bidderView.controller().activate();
            }
        }
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show admin dashboard.
    public void showAdminDashboard(User user) {
        deactivateLiveViews();
        adminView.controller().setUser(user);
        switchView(adminView.root());
        adminView.controller().activate();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show admin homepage manager.
    public void showAdminHomepageManager(User user) {
        deactivateLiveViews();
        adminHomepageView.controller().setUser(user);
        switchView(adminHomepageView.root());
        adminHomepageView.controller().activate();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show change password dialog.
    public void showChangePasswordDialog(User user) {
        LoadedView<ChangePasswordDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "change-password-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "CHANGE PASSWORD", view.root(), 440, 320);
        view.controller().setDialogStage(dialog);
        view.controller().setAuthController(authController);
        view.controller().setUser(user);
        view.controller().setSuccessHandler(message -> NotificationUtil.success(stage, "NOTIFICATION", message));
        dialog.showAndWait();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show bid history dialog.
    public void showBidHistoryDialog(AuctionItem auctionItem, List<BidTransaction> bids) {
        LoadedView<BidHistoryDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "bid-history-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "BID HISTORY", view.root(), 720, 460);
        view.controller().setDialogStage(dialog);
        view.controller().setAuction(auctionItem);
        view.controller().setBids(bids);
        dialog.showAndWait();
    }
    // Phuong thuc: thuc hien chuc nang wire controllers trong lop AuthFrame.
    private void wireControllers() {
        homeView.controller().setShowLoginHandler(this::showLogin);
        homeView.controller().setShowRegisterHandler(this::showRegister);
        homeView.controller().setAuctionController(auctionController);
        homeView.controller().setHomepageController(homepageController);

        loginView.controller().setAuthController(authController);
        loginView.controller().setShowHomeHandler(this::showHome);
        loginView.controller().setShowRegisterHandler(this::showRegister);
        loginView.controller().setLoginSuccessHandler(this::showRoleDashboard);
        loginView.controller().setInfoHandler(message -> NotificationUtil.info(stage, "NOTIFICATION", message));
        loginView.controller().setErrorHandler(message -> NotificationUtil.error(stage, "LOGIN FAILED", message));

        registerView.controller().setAuthController(authController);
        registerView.controller().setShowHomeHandler(this::showHome);
        registerView.controller().setBackToLoginHandler(this::showLogin);
        registerView.controller().setSuccessHandler(message -> NotificationUtil.success(stage, "SUCCESS", message));
        registerView.controller().setWarningHandler(message -> NotificationUtil.warning(stage, "NOTIFICATION", message));
        registerView.controller().setErrorHandler(message -> NotificationUtil.error(stage, "ERROR", message));

        adminView.controller().setFrame(this);
        adminView.controller().setAuthController(authController);
        adminView.controller().setAuctionController(auctionController);
        adminView.controller().setHomepageController(homepageController);

        adminHomepageView.controller().setFrame(this);
        adminHomepageView.controller().setAuctionController(auctionController);
        adminHomepageView.controller().setHomepageController(homepageController);

        sellerView.controller().setFrame(this);
        sellerView.controller().setAuctionController(auctionController);

        bidderView.controller().setFrame(this);
        bidderView.controller().setAuctionController(auctionController);
        bidderView.controller().setAutobidController(autobidController);
        bidderView.controller().setWalletController(walletController);
    }
    // Phuong thuc: thuc hien chuc nang deactivate live views trong lop AuthFrame.
    private void deactivateLiveViews() {
        homeView.controller().deactivate();
        adminView.controller().deactivate();
        adminHomepageView.controller().deactivate();
        bidderView.controller().deactivate();
        sellerView.controller().deactivate();
    }
    // Phuong thuc: thuc hien chuc nang switch view trong lop AuthFrame.
    private void switchView(Parent root) {
        shellController.setContent(root, true);
        if (stage.isMaximized() || stage.isFullScreen()) {
            return;
        }

        if (root instanceof javafx.scene.layout.Region region) {
            stage.setWidth(Math.max(stage.getMinWidth(), region.prefWidth(-1)));
            stage.setHeight(Math.max(stage.getMinHeight(), region.prefHeight(-1)));
            stage.centerOnScreen();
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply language.
    private void applyLanguage(Parent root) {
        UiText.apply(root);
    }
}
