package userauth.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import userauth.controller.AuthController;
import userauth.exception.UnauthorizedException;
import userauth.model.User;

public class LoginPanel extends BorderPane {
    private final AuthFrame frame;
    private final AuthController controller;

    private final TextField txtUsername;
    private final PasswordField txtPassword;

    public LoginPanel(AuthFrame frame, AuthController controller) {
        this.frame = frame;
        this.controller = controller;

        UITheme.stylePage(this);

        HBox content = new HBox(18);

        javafx.scene.layout.StackPane hero = UITheme.createHero(
                "TRUNG TAM DAU GIA",
                "Nen tang dau gia truc quan, nhanh va an toan cho nguoi ban va nguoi mua."
        );

        BorderPane loginCard = UITheme.createSection("DANG NHAP TAI KHOAN");
        loginCard.setMaxWidth(440);

        VBox form = new VBox(10);
        form.setPadding(new Insets(4, 0, 0, 0));

        Label userLabel = UITheme.createFieldLabel("Ten dang nhap");
        txtUsername = new TextField();
        UITheme.styleTextField(txtUsername);

        Label passLabel = UITheme.createFieldLabel("Mat khau");
        txtPassword = new PasswordField();
        UITheme.styleTextField(txtPassword);

        HBox actions = new HBox(10);
        Button btnLogin = new Button("DANG NHAP");
        btnLogin.setPrefWidth(140);
        UITheme.stylePrimaryButton(btnLogin);

        Button btnRegister = new Button("TAO TAI KHOAN");
        UITheme.styleGhostButton(btnRegister);
        actions.getChildren().addAll(btnLogin, btnRegister);

        form.getChildren().addAll(userLabel, txtUsername, passLabel, txtPassword, actions);
        loginCard.setCenter(form);

        VBox rightBox = new VBox(loginCard);
        rightBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        HBox.setHgrow(hero, Priority.ALWAYS);
        hero.setMaxWidth(Double.MAX_VALUE);
        rightBox.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(hero, rightBox);
        HBox.setHgrow(hero, Priority.ALWAYS);
        setCenter(content);

        btnLogin.setOnAction(event -> doLogin());
        btnRegister.setOnAction(event -> {
            clearInputs();
            frame.showRegister();
        });
        txtPassword.setOnAction(event -> doLogin());
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        try {
            User user = controller.login(username, password);
            NotificationUtil.success(frame.getWindow(), "THANH CONG", "Dang nhap thanh cong. Xin chao " + user.getRoleName() + ".");
            clearInputs();
            frame.showRoleDashboard(user);
        } catch (UnauthorizedException ex) {
            NotificationUtil.error(frame.getWindow(), "DANG NHAP THAT BAI", ex.getMessage());
        }
    }

    private void clearInputs() {
        txtUsername.clear();
        txtPassword.clear();
    }
}
