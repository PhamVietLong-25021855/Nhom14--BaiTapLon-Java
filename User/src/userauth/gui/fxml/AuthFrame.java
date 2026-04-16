package userauth.gui.fxml;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
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
    private final Scene scene;
    private final AppShellController shellController;

    private final LoadedView<HomeViewController> homeView;
    private final LoadedView<LoginViewController> loginView;
    private final LoadedView<RegisterViewController> registerView;
    private final LoadedView<AdminDashboardViewController> adminView;
    private final LoadedView<SellerDashboardViewController> sellerView;
    private final LoadedView<BidderDashboardViewController> bidderView;

    public AuthFrame(Stage stage, AuthController authController, AuctionController auctionController) {
        this.stage = stage;
        this.authController = authController;
        this.auctionController = auctionController;

        stage.setTitle("HE THONG DAU GIA SAN PHAM");
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
            case ADMIN -> {
                deactivateLiveViews();
                adminView.controller().setUser(user);
                adminView.controller().refreshData();
                adminView.controller().activate();
                switchView(adminView.root());
            }
            case SELLER -> {
                deactivateLiveViews();
                sellerView.controller().setUser(user);
                sellerView.controller().refreshData();
                switchView(sellerView.root());
            }
            case BIDDER -> {
                deactivateLiveViews();
                bidderView.controller().setUser(user);
                bidderView.controller().refreshData();
                bidderView.controller().activate();
                switchView(bidderView.root());
            }
        }
    }

    public void showChangePasswordDialog(User user) {
        LoadedView<ChangePasswordDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "change-password-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "DOI MAT KHAU", view.root(), 440, 320);
        view.controller().setDialogStage(dialog);
        view.controller().setAuthController(authController);
        view.controller().setUser(user);
        view.controller().setSuccessHandler(message -> NotificationUtil.success(stage, "THONG BAO", message));
        dialog.showAndWait();
    }

    public void showBidHistoryDialog(AuctionItem auctionItem, List<BidTransaction> bids) {
        LoadedView<BidHistoryDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "bid-history-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "LICH SU BID", view.root(), 720, 460);
        view.controller().setDialogStage(dialog);
        view.controller().setAuction(auctionItem);
        view.controller().setBids(bids);
        dialog.showAndWait();
    }

    private void wireControllers() {
        homeView.controller().setShowLoginHandler(this::showLogin);
        homeView.controller().setShowRegisterHandler(this::showRegister);

        loginView.controller().setAuthController(authController);
        loginView.controller().setShowHomeHandler(this::showHome);
        loginView.controller().setShowRegisterHandler(this::showRegister);
        loginView.controller().setLoginSuccessHandler(this::showRoleDashboard);
        loginView.controller().setInfoHandler(message -> NotificationUtil.info(stage, "THONG BAO", message));
        loginView.controller().setErrorHandler(message -> NotificationUtil.error(stage, "DANG NHAP THAT BAI", message));

        registerView.controller().setAuthController(authController);
        registerView.controller().setShowHomeHandler(this::showHome);
        registerView.controller().setBackToLoginHandler(this::showLogin);
        registerView.controller().setSuccessHandler(message -> NotificationUtil.success(stage, "THANH CONG", message));
        registerView.controller().setWarningHandler(message -> NotificationUtil.warning(stage, "THONG BAO", message));
        registerView.controller().setErrorHandler(message -> NotificationUtil.error(stage, "LOI", message));

        adminView.controller().setFrame(this);
        adminView.controller().setAuthController(authController);
        adminView.controller().setAuctionController(auctionController);

        sellerView.controller().setFrame(this);
        sellerView.controller().setAuctionController(auctionController);

        bidderView.controller().setFrame(this);
        bidderView.controller().setAuctionController(auctionController);
    }

    private void deactivateLiveViews() {
        homeView.controller().deactivate();
        adminView.controller().deactivate();
        bidderView.controller().deactivate();
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
}
