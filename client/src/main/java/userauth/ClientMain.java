package userauth;

import userauth.controller.*;

import javafx.application.Application;
import javafx.stage.Stage;
import userauth.api.AuctionApi;
import userauth.api.AuthApi;
import userauth.api.AutobidApi;
import userauth.api.HomepageContentApi;
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

        System.out.println("[Client] Remote mode: using server " + RemoteClientConfig.host() + ":" + RemoteClientConfig.port());

        AuthController authController = new AuthController(authService);
        AuctionController auctionController = new AuctionController(auctionService);
        AutobidController autobidController = new AutobidController(autobidService);
        HomepageController homepageController = new HomepageController(homepageContentService);

        AuthFrame frame = new AuthFrame(stage, authController, auctionController, homepageController, autobidController);
        frame.show();
        frame.showHome();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
