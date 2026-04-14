package com.example.javafxtest;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import userauth.controller.*;
import userauth.dao.*;
import userauth.exception.UnauthorizedException;
import userauth.gui.*;
import userauth.model.Role;
import userauth.model.User;
import userauth.service.*;
import userauth.validation.UserValidator;

import javax.swing.*;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller extends Application implements Initializable {
    UserDAO userDAO = new UserDAOImpl();
    AuthService authService = new AuthService(userDAO);
    AuthController authController = new AuthController(authService);

    AuctionDAO auctionDAO = new AuctionDAOImpl();
    AuctionService auctionService = new AuctionService(auctionDAO);
    AuctionController auctionController = new AuctionController(auctionService);

    AuctionScheduler scheduler = new userauth.service.AuctionScheduler(auctionService);
    AuthFrame frame;

    @FXML
    private VBox loginPane;

    @FXML
    private ToggleGroup NotLogIn;

    @FXML
    private ToggleButton login;

    @FXML
    private PasswordField loginPassword;

    @FXML
    private  TextField passwordText;

    @FXML
    private TextField loginUsername;



    @FXML
    private ToggleButton register;

    @FXML
    private TextField registerEmail;

    @FXML
    private VBox registerPane;

    @FXML
    private PasswordField registerPassword1;

    @FXML
    private PasswordField registerPassword2;

    @FXML
    private CheckBox passwordCheckBox;

    @FXML
    private TextField registerUsername;

    @FXML
    private ChoiceBox<Role> registerRole;

    @Override
    public void start(Stage primaryStage) throws Exception {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerRole.getItems().addAll(Role.BIDDER, Role.SELLER, Role.ADMIN);
        scheduler.start();

        // Chạy Java Swing trên Event Dispatch Thread (Best Practice)
        SwingUtilities.invokeLater(() -> {
            frame = new AuthFrame(authController, auctionController);
            frame.setVisible(false);
        });
    }

    @FXML
    void toggleButton(ActionEvent event) {
        ToggleButton  scr = (ToggleButton) event.getSource();
        if (scr.getToggleGroup().getSelectedToggle() == null){
            scr.setSelected(true);
            return;
        }

        loginPane.setManaged(false);
        registerPane.setManaged(false);
        if (scr.getId().equals("login")) {
            loginPane.setVisible(true);
            registerPane.setVisible(false);
        } else {
            loginPane.setVisible(false);
            registerPane.setVisible(true);
        }
        ;
    }

    @FXML
    void loginSubmit(ActionEvent event) {
        Stage stage = (Stage) loginPane.getScene().getWindow();
        String username = loginUsername.getText().trim();
        String password;

        if (passwordCheckBox.isSelected()){
            password = passwordText.getText();
        }else {
            password = loginPassword.getText();
        }
        try {
            User user = authController.login(username, password);
            loginUsername.setText("");
            loginPassword.setText("");
            frame.showRoleDashboard(user);
            frame.setVisible(true);
            stage.close();
            frame.toFront();
        } catch (UnauthorizedException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    @FXML
    void registerSubmit(ActionEvent event) {
        String username = registerUsername.getText().trim();
        String fullName = "None";
        String email = registerEmail.getText().trim();
        String password = registerPassword1.getText();
        String confirmPassword = registerPassword2.getText();
        Role role = (Role) registerRole.getValue();


        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            System.out.println("Error: " + "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!UserValidator.isValidEmail(email)) {
            System.out.println("Error: " + "Email không hợp lệ!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            System.out.println("Error: " + "Mật khẩu nhập lại không khớp!");
            return;
        }

        if (!UserValidator.isValidPassword(password)) {
            System.out.println("Error: " + "Password cần ít nhất 6 ký tự, gồm chữ và số!");
            return;
        }

        String result = authController.registerGUI(username, password, fullName, email, role);
        if (result.toLowerCase().contains("thành công") || result.toLowerCase().contains("success")) {
            System.out.println("Success: " + "Đăng ký thành công!\nVui lòng đăng nhập lại.");
            registerUsername.setText("");
            //txtFullName.setText("");
            registerEmail.setText("");
            registerPassword1.setText("");
            registerPassword2.setText("");
            login.setSelected(true);
        } else {
            System.out.println("Error: " +  result);
        }

    }

    @FXML
    void showPassword(ActionEvent event) {
        if (passwordCheckBox.isSelected()) {
            // Show Password
            passwordText.setText(loginPassword.getText());
            passwordText.setVisible(true);
            loginPassword.setVisible(false);
        } else {
            // Hide Password
            loginPassword.setText(passwordText.getText());
            loginPassword.setVisible(true);
            passwordText.setVisible(false);
        }
    }
}

