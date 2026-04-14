package userauth.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import userauth.controller.AuthController;
import userauth.model.Role;
import userauth.validation.UserValidator;

public class RegisterPanel extends BorderPane {
    private final AuthFrame frame;
    private final AuthController controller;

    private final TextField txtUsername;
    private final TextField txtEmail;
    private final TextField txtFullName;
    private final PasswordField txtPassword;
    private final PasswordField txtConfirmPassword;
    private final ComboBox<Role> cbRole;

    public RegisterPanel(AuthFrame frame, AuthController controller) {
        this.frame = frame;
        this.controller = controller;

        UITheme.stylePage(this);

        HBox content = new HBox(18);
        content.getChildren().add(UITheme.createHero(
                "TAO TAI KHOAN",
                "Hoan tat thong tin ben duoi de tham gia he thong dau gia ngay hom nay."
        ));

        BorderPane registerCard = UITheme.createSection("DANG KY TAI KHOAN");
        registerCard.setMaxWidth(520);

        VBox form = new VBox(10);

        Button btnBackTop = new Button("QUAY VE DANG NHAP");
        UITheme.styleGhostButton(btnBackTop);

        txtUsername = new TextField();
        txtFullName = new TextField();
        txtEmail = new TextField();
        txtPassword = new PasswordField();
        txtConfirmPassword = new PasswordField();
        cbRole = new ComboBox<>();
        cbRole.getItems().addAll(Role.BIDDER, Role.SELLER, Role.ADMIN);
        cbRole.getSelectionModel().select(Role.BIDDER);

        UITheme.styleTextField(txtUsername);
        UITheme.styleTextField(txtFullName);
        UITheme.styleTextField(txtEmail);
        UITheme.styleTextField(txtPassword);
        UITheme.styleTextField(txtConfirmPassword);
        UITheme.styleComboBox(cbRole);

        Button btnRegister = new Button("DANG KY");
        btnRegister.setPrefWidth(130);
        UITheme.styleSuccessButton(btnRegister);

        form.getChildren().addAll(
                btnBackTop,
                UITheme.createFieldLabel("Ten dang nhap"),
                txtUsername,
                UITheme.createFieldLabel("Ho va ten"),
                txtFullName,
                UITheme.createFieldLabel("Email"),
                txtEmail,
                UITheme.createFieldLabel("Mat khau"),
                txtPassword,
                UITheme.createFieldLabel("Nhap lai mat khau"),
                txtConfirmPassword,
                UITheme.createFieldLabel("Vai tro"),
                cbRole,
                btnRegister
        );
        registerCard.setCenter(form);

        VBox rightBox = new VBox(registerCard);
        rightBox.setAlignment(Pos.CENTER);
        rightBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        content.getChildren().add(rightBox);
        setCenter(content);

        btnRegister.setOnAction(event -> doRegister());
        btnBackTop.setOnAction(event -> frame.showLogin());
    }

    private void doRegister() {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        Role role = cbRole.getValue();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            NotificationUtil.error(frame.getWindow(), "LOI", "Vui long nhap day du thong tin.");
            return;
        }

        if (!UserValidator.isValidEmail(email)) {
            NotificationUtil.error(frame.getWindow(), "LOI", "Email khong hop le.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            NotificationUtil.error(frame.getWindow(), "LOI", "Mat khau nhap lai khong khop.");
            return;
        }

        if (!UserValidator.isValidPassword(password)) {
            NotificationUtil.error(frame.getWindow(), "LOI", "Mat khau can it nhat 6 ky tu, gom chu va so.");
            return;
        }

        String result = controller.registerGUI(username, password, fullName, email, role);
        if ("SUCCESS".equals(result) || result.toLowerCase().contains("thanh cong")) {
            NotificationUtil.success(frame.getWindow(), "THANH CONG", "Dang ky thanh cong. Vui long dang nhap.");
            clearInputs();
            frame.showLogin();
        } else {
            NotificationUtil.warning(frame.getWindow(), "THONG BAO", result);
        }
    }

    private void clearInputs() {
        txtUsername.clear();
        txtFullName.clear();
        txtEmail.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
        cbRole.getSelectionModel().select(Role.BIDDER);
    }
}
