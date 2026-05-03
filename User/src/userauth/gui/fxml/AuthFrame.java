package userauth.gui.fxml;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.HomepageController;
import userauth.controller.WalletController;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.model.User;

import java.util.List;

public class AuthFrame {
    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 840;
    private static final boolean OPEN_FULLSCREEN = true;

    private final Stage stage;
    private final AuthController authController;
    private final AuctionController auctionController;
    private final HomepageController homepageController;
    private final Scene scene;
    private final AppShellController shellController;
    private final AutobidController autobidController;
    private final WalletController walletController;

    private final LoadedView<HomeViewController> homeView;
    private final LoadedView<LoginViewController> loginView;
    private final LoadedView<RegisterViewController> registerView;
    private final LoadedView<AdminDashboardViewController> adminView;
    private final LoadedView<AdminHomepageViewController> adminHomepageView;
    private final LoadedView<SellerDashboardViewController> sellerView;
    private final LoadedView<BidderDashboardViewController> bidderView;

    public AuthFrame(Stage stage, AuthController authController, AuctionController auctionController, HomepageController homepageController, AutobidController autobidController, WalletController walletController) {
        this.stage = stage;
        this.authController = authController;
        this.auctionController = auctionController;
        this.homepageController = homepageController;
        this.autobidController = autobidController;
        this.walletController = walletController;

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

    public void show() {
        stage.show();
        if (!OPEN_FULLSCREEN) {
            stage.centerOnScreen();
        }
    }

    public Window getWindow() {
        return stage;
    }

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

    public void showHome() {
        deactivateLiveViews();
        switchView(homeView.root());
        homeView.controller().activate();
    }

    public void showLogin() {
        deactivateLiveViews();
        switchView(loginView.root());
    }

    public void showRegister() {
        deactivateLiveViews();
        switchView(registerView.root());
    }

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

    public void showAdminDashboard(User user) {
        deactivateLiveViews();
        adminView.controller().setUser(user);
        switchView(adminView.root());
        adminView.controller().activate();
    }

    public void showAdminHomepageManager(User user) {
        deactivateLiveViews();
        adminHomepageView.controller().setUser(user);
        switchView(adminHomepageView.root());
        adminHomepageView.controller().activate();
    }

    public void showChangePasswordDialog(User user) {
        LoadedView<ChangePasswordDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "change-password-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "CHANGE PASSWORD", view.root(), 440, 320);
        view.controller().setDialogStage(dialog);
        view.controller().setAuthController(authController);
        view.controller().setUser(user);
        view.controller().setSuccessHandler(message -> NotificationUtil.success(stage, "NOTIFICATION", message));
        dialog.showAndWait();
    }

    public void showBidHistoryDialog(AuctionItem auctionItem, List<BidTransaction> bids) {
        LoadedView<BidHistoryDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "bid-history-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "BID HISTORY", view.root(), 720, 460);
        view.controller().setDialogStage(dialog);
        view.controller().setAuction(auctionItem);
        view.controller().setBids(bids);
        dialog.showAndWait();
    }

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

    private void deactivateLiveViews() {
        homeView.controller().deactivate();
        adminView.controller().deactivate();
        adminHomepageView.controller().deactivate();
        bidderView.controller().deactivate();
        sellerView.controller().deactivate();
    }

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

    private void applyLanguage(Parent root) {
        UiText.apply(root);
    }
}
