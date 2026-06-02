package userauth.gui.fxml.shell;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import userauth.controller.*;
import userauth.gui.fxml.admin.AdminDashboardViewController;
import userauth.gui.fxml.admin.AdminHomepageViewController;
import userauth.gui.fxml.auth.LoginViewController;
import userauth.gui.fxml.auth.RegisterViewController;
import userauth.gui.fxml.bidder.BidderDashboardViewController;
import userauth.gui.fxml.dialog.*;
import userauth.gui.fxml.home.HomeViewController;
import userauth.gui.fxml.seller.SellerDashboardViewController;
import userauth.gui.fxml.shared.*;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.model.Notification;
import userauth.model.User;
import userauth.remote.RemoteNotificationService;

import java.util.List;
import java.util.function.Consumer;

public class AuthFrame {
    private static final boolean OPEN_FULLSCREEN = false;
    private static final double DEFAULT_WIDTH = 1000;
    private static final double DEFAULT_HEIGHT = 700;
    private static final double SESSION_CHECK_INTERVAL_SECONDS = 10.0;

    private final Stage stage;
    private final AuthController authController;
    private final AuctionController auctionController;
    private final HomepageController homepageController;
    private final Scene scene;
    private final AppShellController shellController;
    private final AutobidController autobidController;
    private final WalletController walletController;
    private final NotificationController notificationController;

    private final LoadedView<HomeViewController> homeView;
    private final LoadedView<LoginViewController> loginView;
    private final LoadedView<RegisterViewController> registerView;
    private final LoadedView<AdminDashboardViewController> adminView;
    private final LoadedView<AdminHomepageViewController> adminHomepageView;
    private final LoadedView<SellerDashboardViewController> sellerView;
    private final LoadedView<BidderDashboardViewController> bidderView;
    private Timeline sessionCheckTimeline;
    private User currentUser;
    private boolean sessionCheckInProgress;
    private boolean forcedLogoutInProgress;

    public AuthFrame(Stage stage, AuthController authController, AuctionController auctionController,
                     HomepageController homepageController, AutobidController autobidController,
                     WalletController walletController, NotificationController notificationController) {
        this.stage = stage;
        this.authController = authController;
        this.auctionController = auctionController;
        this.homepageController = homepageController;
        this.autobidController = autobidController;
        this.walletController = walletController;
        this.notificationController = notificationController;

        stage.setTitle(UiText.text("PRODUCT AUCTION PLATFORM"));
        try (java.io.InputStream iconStream = AuthFrame.class.getResourceAsStream("/userauth/gui/fxml/shared/app-icon.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new javafx.scene.image.Image(iconStream));
            } else {
                System.err.println("[UI Icon] Không tìm thấy file app-icon.png tại đường dẫn chỉ định.");
            }
        } catch (Exception e) {
            System.err.println("[UI Icon] Lỗi nạp icon cho ứng dụng: " + e.getMessage());
        }
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.setMaximized(true);

        LoadedView<AppShellController> shellView = FxmlRuntime.loadView(AuthFrame.class, "shell/app-shell.fxml", "view");
        this.shellController = shellView.controller();

        homeView = FxmlRuntime.loadView(AuthFrame.class, "home/home-view.fxml", "view");
        loginView = FxmlRuntime.loadView(AuthFrame.class, "auth/login-view.fxml", "view");
        registerView = FxmlRuntime.loadView(AuthFrame.class, "auth/register-view.fxml", "view");
        adminView = FxmlRuntime.loadView(AuthFrame.class, "admin/admin-dashboard-view.fxml", "view");
        adminHomepageView = FxmlRuntime.loadView(AuthFrame.class, "admin/admin-homepage-view.fxml", "view");
        sellerView = FxmlRuntime.loadView(AuthFrame.class, "seller/seller-dashboard-view.fxml", "view");
        bidderView = FxmlRuntime.loadView(AuthFrame.class, "bidder/bidder-dashboard-view.fxml", "view");

        wireControllers();
        configureSessionMonitor();

        shellController.setContent(homeView.root());
        scene = new Scene(shellView.root(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
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
        stopSessionMonitor();
        currentUser = null;
        deactivateLiveViews();
        switchView(homeView.root());
        homeView.controller().activate();
    }

    public void showLogin() {
        stopSessionMonitor();
        currentUser = null;
        deactivateLiveViews();
        authController.logout();
        switchView(loginView.root());
    }

    public void showRegister() {
        stopSessionMonitor();
        currentUser = null;
        deactivateLiveViews();
        switchView(registerView.root());
    }

    public void showRoleDashboard(User user) {
        if (user == null) {
            NotificationUtil.error(stage, "LOGIN FAILED", "Login failed.");
            showLogin();
            return;
        }
        switch (user.getRole()) {
            case ADMIN -> showAdminDashboard(user);
            case SELLER -> {
                deactivateLiveViews();
                startSessionMonitor(user);
                sellerView.controller().setUser(user);
                switchView(sellerView.root());
                sellerView.controller().activate();
            }
            case BIDDER -> {
                deactivateLiveViews();
                startSessionMonitor(user);
                bidderView.controller().setUser(user);
                switchView(bidderView.root());
                bidderView.controller().activate();
            }
        }
    }

    public void showAdminDashboard(User user) {
        deactivateLiveViews();
        startSessionMonitor(user);
        adminView.controller().setUser(user);
        switchView(adminView.root());
        adminView.controller().activate();
    }

    public void showAdminHomepageManager(User user) {
        deactivateLiveViews();
        startSessionMonitor(user);
        adminHomepageView.controller().setUser(user);
        switchView(adminHomepageView.root());
        adminHomepageView.controller().activate();
    }

    public void showChangePasswordDialog(User user) {
        LoadedView<ChangePasswordDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "dialog/change-password-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "CHANGE PASSWORD", view.root(), 820, 700);
        view.controller().setDialogStage(dialog);
        view.controller().setAuthController(authController);
        view.controller().setUser(user);
        view.controller().setSuccessHandler(message -> NotificationUtil.success(stage, "NOTIFICATION", message));
        dialog.showAndWait();
    }

    public void showProfileDialog(User user, Consumer<User> profileUpdatedHandler) {
        LoadedView<ProfileDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "dialog/profile-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "EDIT PROFILE", view.root(), 820, 670);
        view.controller().setDialogStage(dialog);
        view.controller().setAuthController(authController);
        view.controller().setUser(user);
        view.controller().setSuccessHandler(updatedUser -> {
            if (profileUpdatedHandler != null) {
                profileUpdatedHandler.accept(updatedUser);
            }
            NotificationUtil.success(stage, "NOTIFICATION", "Profile updated successfully.");
        });
        dialog.showAndWait();
    }

    public void showBidHistoryDialog(User bidder, List<AuctionItem> auctions, List<BidTransaction> bids) {
        LoadedView<BidHistoryDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "dialog/bid-history-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "BID HISTORY", view.root(), 1040, 720);
        view.controller().setDialogStage(dialog);
        view.controller().setBidderHistory(bidder, auctions, bids);
        dialog.showAndWait();
    }

    public void showInboxDialog(User user, List<Notification> notificationList) {
        LoadedView<InboxDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "dialog/inbox.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "INBOX", view.root(), 1040, 720);
        view.controller().setDialogStage(dialog);
        view.controller().setNotificationContext(notificationController, user == null ? -1 : user.getId());
        view.controller().loadNotifications(notificationList);
        dialog.showAndWait();
    }

    public void showTopUpDialog(User user, Runnable successHandler) {
        LoadedView<TopUpDialogController> view = FxmlRuntime.loadView(AuthFrame.class, "dialog/top-up-dialog.fxml", "dialog");
        Stage dialog = FxmlRuntime.createModalDialog(stage, "TOP UP WALLET", view.root(), 560, 430);
        view.controller().setDialogStage(dialog);
        view.controller().setWalletController(walletController);
        view.controller().setUser(user);
        view.controller().setSuccessHandler(message -> {
            if (successHandler != null) {
                successHandler.run();
            }
            NotificationUtil.success(stage, "NOTIFICATION", message);
        });
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
        sellerView.controller().setAuthController(authController);
        sellerView.controller().setAuctionController(auctionController);
        sellerView.controller().setNotificationController(notificationController);

        bidderView.controller().setFrame(this);
        bidderView.controller().setAuthController(authController);
        bidderView.controller().setAuctionController(auctionController);
        bidderView.controller().setAutobidController(autobidController);
        bidderView.controller().setWalletController(walletController);
        bidderView.controller().setNotificationController(notificationController);
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
    }

    private void configureSessionMonitor() {
        sessionCheckTimeline = new Timeline(
                new KeyFrame(Duration.seconds(SESSION_CHECK_INTERVAL_SECONDS), event -> verifyCurrentSession())
        );
        sessionCheckTimeline.setCycleCount(Animation.INDEFINITE);
    }

    private void startSessionMonitor(User user) {
        currentUser = user;
        forcedLogoutInProgress = false;
        sessionCheckInProgress = false;
        if (user == null) {
            return;
        }
        if (sessionCheckTimeline.getStatus() != Animation.Status.RUNNING) {
            sessionCheckTimeline.play();
        }
        verifyCurrentSession();
    }

    private void stopSessionMonitor() {
        if (sessionCheckTimeline != null) {
            sessionCheckTimeline.stop();
        }
        sessionCheckInProgress = false;
        forcedLogoutInProgress = false;
    }

    private void verifyCurrentSession() {
        User user = currentUser;
        if (user == null || sessionCheckInProgress || forcedLogoutInProgress) {
            return;
        }

        int checkedUserId = user.getId();
        sessionCheckInProgress = true;
        UiAsync.run(
                () -> authController.getUserById(checkedUserId),
                refreshedUser -> {
                    sessionCheckInProgress = false;
                    if (!isStillCurrentUser(checkedUserId)) {
                        return;
                    }
                    if (refreshedUser == null || "BLOCKED".equals(refreshedUser.getStatus())) {
                        forceLogoutToLogin("Your account has been locked or deleted. Please log in again.");
                    }
                },
                error -> {
                    sessionCheckInProgress = false;
                    if (!isStillCurrentUser(checkedUserId)) {
                        return;
                    }
                    if (isInvalidSessionError(error)) {
                        forceLogoutToLogin("Your account has been locked or deleted. Please log in again.");
                    }
                }
        );
    }

    private boolean isStillCurrentUser(int checkedUserId) {
        return currentUser != null && currentUser.getId() == checkedUserId && !forcedLogoutInProgress;
    }

    private boolean isInvalidSessionError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current.getClass().getSimpleName().contains("UnauthorizedException")) {
                return true;
            }
            current = current.getCause();
        }

        String message = error == null || error.getMessage() == null ? "" : error.getMessage().toLowerCase(java.util.Locale.ROOT);
        return message.contains("session")
                || message.contains("authentication is required")
                || message.contains("please log in again")
                || message.contains("user not found")
                || message.contains("account has been locked");
    }

    private void forceLogoutToLogin(String message) {
        if (forcedLogoutInProgress) {
            return;
        }
        forcedLogoutInProgress = true;
        showLogin();
        NotificationUtil.warning(stage, "SESSION EXPIRED", message);
    }

    private void applyLanguage(Parent root) {
        UiText.apply(root);
    }
}
