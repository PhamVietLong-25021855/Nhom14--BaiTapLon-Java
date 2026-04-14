package userauth.gui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.model.User;

import java.util.Optional;

public class AuthFrame {
    private final Stage stage;
    private final AnimatedCardPanel mainPanel;

    private final AuthController authController;
    private final AuctionController auctionController;

    private final LoginPanel loginPanel;
    private final RegisterPanel registerPanel;
    private final DashboardPanel adminDashboard;
    private final SellerPanel sellerPanel;
    private final BidderPanel bidderPanel;

    public AuthFrame(Stage stage, AuthController authController, AuctionController auctionController) {
        this.stage = stage;
        this.authController = authController;
        this.auctionController = auctionController;

        stage.setTitle("HE THONG DAU GIA SAN PHAM");
        stage.setMinWidth(980);
        stage.setMinHeight(700);

        mainPanel = new AnimatedCardPanel();

        StackPane root = new StackPane(mainPanel);
        UITheme.stylePage(root);
        root.setPadding(new Insets(18));

        Scene scene = new Scene(root, 980, 700);
        stage.setScene(scene);

        loginPanel = new LoginPanel(this, this.authController);
        registerPanel = new RegisterPanel(this, this.authController);
        adminDashboard = new DashboardPanel(this, this.authController);
        sellerPanel = new SellerPanel(this, this.auctionController);
        bidderPanel = new BidderPanel(this, this.auctionController);

        mainPanel.addCard(loginPanel, "LOGIN");
        mainPanel.addCard(registerPanel, "REGISTER");
        mainPanel.addCard(adminDashboard, "ADMIN");
        mainPanel.addCard(sellerPanel, "SELLER");
        mainPanel.addCard(bidderPanel, "BIDDER");
    }

    public void show() {
        stage.show();
        stage.centerOnScreen();
    }

    public Window getWindow() {
        return stage;
    }

    public void showLogin() {
        bidderPanel.deactivate();
        prepareStage(980, 700);
        mainPanel.showCard("LOGIN");
    }

    public void showRegister() {
        bidderPanel.deactivate();
        prepareStage(1060, 760);
        mainPanel.showCard("REGISTER");
    }

    public void showRoleDashboard(User user) {
        switch (user.getRole()) {
            case ADMIN -> {
                bidderPanel.deactivate();
                prepareStage(1120, 740);
                adminDashboard.setUser(user);
                adminDashboard.refreshData();
                mainPanel.showCard("ADMIN");
            }
            case SELLER -> {
                bidderPanel.deactivate();
                prepareStage(1220, 780);
                sellerPanel.setUser(user);
                sellerPanel.refreshData();
                mainPanel.showCard("SELLER");
            }
            case BIDDER -> {
                prepareStage(1160, 740);
                bidderPanel.setUser(user);
                bidderPanel.refreshData();
                bidderPanel.activate();
                mainPanel.showCard("BIDDER");
            }
        }
    }

    public void showChangePasswordDialog(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("DOI MAT KHAU");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label oldLabel = UITheme.createFieldLabel("Mat khau hien tai");
        PasswordField oldPasswordField = new PasswordField();
        UITheme.styleTextField(oldPasswordField);

        Label newLabel = UITheme.createFieldLabel("Mat khau moi");
        PasswordField newPasswordField = new PasswordField();
        UITheme.styleTextField(newPasswordField);

        VBox form = new VBox(10, oldLabel, oldPasswordField, newLabel, newPasswordField);
        form.setPadding(new Insets(6, 4, 6, 4));

        Label title = new Label("DOI MAT KHAU");
        title.setFont(UITheme.sectionTitleFont());
        title.setTextFill(UITheme.TEXT_PRIMARY);

        VBox wrapper = new VBox(14, title, form);
        wrapper.setPadding(new Insets(8, 6, 8, 6));
        pane.setContent(wrapper);
        pane.setStyle(
                "-fx-background-color: " + UITheme.toRgb(UITheme.CARD_BG) + ";" +
                "-fx-border-color: " + UITheme.toRgb(UITheme.WARNING) + ";" +
                "-fx-border-radius: 18;" +
                "-fx-background-radius: 18;"
        );

        for (ButtonType buttonType : pane.getButtonTypes()) {
            Button button = (Button) pane.lookupButton(buttonType);
            if (button == null) {
                continue;
            }
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                UITheme.stylePrimaryButton(button);
            } else {
                UITheme.styleGhostButton(button);
            }
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String oldPass = oldPasswordField.getText().trim();
        String newPass = newPasswordField.getText().trim();
        if (oldPass.isEmpty() || newPass.isEmpty()) {
            NotificationUtil.warning(stage, "LOI", "Vui long nhap day du mat khau.");
            return;
        }

        String changeResult = authController.changePassword(user.getUsername(), oldPass, newPass);
        if ("SUCCESS".equals(changeResult)) {
            NotificationUtil.success(stage, "THONG BAO", "Doi mat khau thanh cong.");
        } else {
            NotificationUtil.error(stage, "LOI", changeResult);
        }
    }

    private void prepareStage(double width, double height) {
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();
    }
}
