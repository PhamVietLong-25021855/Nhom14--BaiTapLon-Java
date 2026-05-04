package userauth;

import javafx.application.Application;
import javafx.stage.Stage;
import userauth.client.network.RemoteApiClient;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.controller.AutobidController;
import userauth.controller.HomepageController;
import userauth.controller.WalletController;
import userauth.gui.fxml.AuthFrame;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        RemoteApiClient remoteApiClient = RemoteApiClient.fromConfiguration();
        AuthController authController = new AuthController(remoteApiClient);
        WalletController walletController = new WalletController(remoteApiClient);
        AutobidController autobidController = new AutobidController(remoteApiClient);
        AuctionController auctionController = new AuctionController(remoteApiClient);
        HomepageController homepageController = new HomepageController(remoteApiClient);

        AuthFrame frame = new AuthFrame(stage, authController, auctionController, homepageController, autobidController, walletController);
        frame.show();
        frame.showHome();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
