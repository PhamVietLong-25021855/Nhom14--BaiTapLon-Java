package userauth;

import userauth.api.*;
import userauth.controller.*;

import javafx.application.Application;
import javafx.stage.Stage;
import userauth.model.Notification;
import userauth.remote.*;
import userauth.gui.fxml.shell.AuthFrame;

public class ClientMain extends Application {
    @Override
    public void start(Stage stage) {
        RemoteAuctionClient remoteClient = new RemoteAuctionClient();
        AuthApi authService = new RemoteAuthService(remoteClient);
        AuctionApi auctionService = new RemoteAuctionService(remoteClient);
        AutobidApi autobidService = new RemoteAutobidService(remoteClient);
        HomepageContentApi homepageContentService = new RemoteHomepageContentService(remoteClient);
        WalletApi walletService = new RemoteWalletService(remoteClient);
        NotificationApi notificationApi = new RemoteNotificationService(remoteClient);

        System.out.println("[Client] Remote mode: using server " + RemoteClientConfig.host() + ":" + RemoteClientConfig.port());

        AuthController authController = new AuthController(authService);
        AuctionController auctionController = new AuctionController(auctionService);
        AutobidController autobidController = new AutobidController(autobidService);
        HomepageController homepageController = new HomepageController(homepageContentService);
        WalletController walletController = new WalletController(walletService);
        NotificationController notificationController = new NotificationController(notificationApi);

        AuthFrame frame = new AuthFrame(
                stage,
                authController,
                auctionController,
                homepageController,
                autobidController,
                walletController,
                notificationController
        );
        frame.show();
        frame.showHome();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
