package userauth;

import javafx.application.Application;
import javafx.stage.Stage;
import userauth.api.AuctionApi;
import userauth.api.AuthApi;
import userauth.api.AutobidApi;
import userauth.api.HomepageContentApi;
import userauth.client.remote.RemoteAuctionClient;
import userauth.client.remote.RemoteAuctionService;
import userauth.client.remote.RemoteAuthService;
import userauth.client.remote.RemoteAutobidService;
import userauth.client.remote.RemoteClientConfig;
import userauth.client.remote.RemoteHomepageContentService;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.HomepageController;
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
