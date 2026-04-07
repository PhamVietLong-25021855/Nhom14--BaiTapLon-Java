package userauth.gui;

import userauth.controller.AuthController;
import userauth.model.User;
import userauth.exception.UnauthorizedException;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    private AuthFrame frame;
    private AuthController controller;

    private JTextField txtUsername;
    private JPasswordField txtPassword;

    public LoginPanel(AuthFrame frame, AuthController controller) {
        this.frame = frame;
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(UITheme.APP_BG);

        JLabel lblTitle = new JLabel("HỆ THỐNG ĐẤU GIÁ", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.titleFont());
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(30, 0, 6, 0));
        JLabel lblSub = new JLabel("Đăng nhập để tiếp tục", SwingConstants.CENTER);
        lblSub.setFont(UITheme.bodyFont());
        lblSub.setForeground(Color.BLACK);
        lblSub.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.APP_BG);
        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel loginSection = UITheme.createRoundedSection("Thông tin đăng nhập", new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setPreferredSize(new Dimension(360, 220));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(UITheme.labelFont());
        lblUser.setForeground(UITheme.TEXT_SECONDARY);
        formPanel.add(lblUser, gbc);

        gbc.gridy = 1;
        txtUsername = new JTextField(20);
        UITheme.styleTextField(txtUsername);
        formPanel.add(txtUsername, gbc);

        gbc.gridy = 2;
        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(UITheme.labelFont());
        lblPass.setForeground(UITheme.TEXT_SECONDARY);
        formPanel.add(lblPass, gbc);

        gbc.gridy = 3;
        txtPassword = new JPasswordField(20);
        UITheme.styleTextField(txtPassword);
        formPanel.add(txtPassword, gbc);
        loginSection.add(formPanel, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UITheme.APP_BG);
        wrapper.add(loginSection);
        add(wrapper, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(UITheme.APP_BG);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 34, 0));

        JButton btnLogin = new JButton("Đăng Nhập");
        UITheme.stylePrimaryButton(btnLogin);
        btnLogin.setPreferredSize(new Dimension(140, 40));

        JButton btnRegister = new JButton("Tạo tài khoản");
        UITheme.styleGhostButton(btnRegister);

        btnLogin.addActionListener(e -> {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            try {
                User user = controller.login(username, password);
                NotificationUtil.success(frame, "Thành công", "Đăng nhập thành công!\nXin chào " + user.getRole());
                txtUsername.setText("");
                txtPassword.setText("");
                frame.showRoleDashboard(user);
            } catch (UnauthorizedException ex) {
                NotificationUtil.error(frame, "Lỗi", ex.getMessage());
            }
        });

        btnRegister.addActionListener(e -> {
            txtUsername.setText("");
            txtPassword.setText("");
            frame.showRegister();
        });

        btnPanel.add(btnLogin);
        btnPanel.add(btnRegister);

        add(btnPanel, BorderLayout.SOUTH);
    }
}
