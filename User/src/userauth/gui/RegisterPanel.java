package userauth.gui;

import userauth.controller.AuthController;
import userauth.model.Role;
import userauth.validation.UserValidator;

import javax.swing.*;
import java.awt.*;

public class RegisterPanel extends JPanel {
    private AuthFrame frame;
    private AuthController controller;

    private JTextField txtUsername;
    private JTextField txtEmail;
    private JTextField txtFullName;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JComboBox<Role> cbRole;

    public RegisterPanel(AuthFrame frame, AuthController controller) {
        this.frame = frame;
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(UITheme.APP_BG);

        JLabel lblTitle = new JLabel("ĐĂNG KÝ TÀI KHOẢN", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.sectionTitleFont());
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 6, 0));
        JLabel lblSub = new JLabel("Tạo tài khoản nhanh chóng và bảo mật", SwingConstants.CENTER);
        lblSub.setFont(UITheme.bodyFont());
        lblSub.setForeground(Color.BLACK);
        lblSub.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.APP_BG);
        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        JPanel registerSection = UITheme.createRoundedSection("Thông tin tài khoản", new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 12, 4, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(UITheme.labelFont());
        lblUsername.setForeground(UITheme.TEXT_SECONDARY);
        formPanel.add(lblUsername, gbc);
        txtUsername = new JTextField(20);
        UITheme.styleTextField(txtUsername);
        gbc.gridy = 1;
        formPanel.add(txtUsername, gbc);

        gbc.gridy = 2;
        JLabel lblFullName = new JLabel("Họ tên:");
        lblFullName.setFont(UITheme.labelFont());
        lblFullName.setForeground(UITheme.TEXT_SECONDARY);
        formPanel.add(lblFullName, gbc);
        txtFullName = new JTextField(20);
        UITheme.styleTextField(txtFullName);
        gbc.gridy = 3;
        formPanel.add(txtFullName, gbc);

        gbc.gridy = 4;
        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(UITheme.labelFont());
        lblEmail.setForeground(UITheme.TEXT_SECONDARY);
        formPanel.add(lblEmail, gbc);
        txtEmail = new JTextField(20);
        UITheme.styleTextField(txtEmail);
        gbc.gridy = 5;
        formPanel.add(txtEmail, gbc);

        gbc.gridy = 6;
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(UITheme.labelFont());
        lblPassword.setForeground(UITheme.TEXT_SECONDARY);
        formPanel.add(lblPassword, gbc);
        txtPassword = new JPasswordField(20);
        UITheme.styleTextField(txtPassword);
        gbc.gridy = 7;
        formPanel.add(txtPassword, gbc);

        gbc.gridy = 8;
        JLabel lblConfirmPassword = new JLabel("Nhập lại Password:");
        lblConfirmPassword.setFont(UITheme.labelFont());
        lblConfirmPassword.setForeground(UITheme.TEXT_SECONDARY);
        formPanel.add(lblConfirmPassword, gbc);
        txtConfirmPassword = new JPasswordField(20);
        UITheme.styleTextField(txtConfirmPassword);
        gbc.gridy = 9;
        formPanel.add(txtConfirmPassword, gbc);

        gbc.gridy = 10;
        JLabel lblRole = new JLabel("Vai trò (Role):");
        lblRole.setFont(UITheme.labelFont());
        lblRole.setForeground(UITheme.TEXT_SECONDARY);
        formPanel.add(lblRole, gbc);
        cbRole = new JComboBox<>(new Role[] { Role.BIDDER, Role.SELLER, Role.ADMIN });
        UITheme.styleComboBox(cbRole);
        gbc.gridy = 11;
        formPanel.add(cbRole, gbc);
        registerSection.add(formPanel, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UITheme.APP_BG);
        wrapper.add(registerSection);
        add(wrapper, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(UITheme.APP_BG);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 24, 0));

        JButton btnRegister = new JButton("Đăng Ký");
        UITheme.styleSuccessButton(btnRegister);
        btnRegister.setPreferredSize(new Dimension(120, 35));

        JButton btnBack = new JButton("Quay lại Đăng Nhập");
        UITheme.styleGhostButton(btnBack);

        btnRegister.addActionListener(e -> {
            String username = txtUsername.getText().trim();
            String fullName = txtFullName.getText().trim();
            String email = txtEmail.getText().trim();
            String password = new String(txtPassword.getPassword());
            String confirmPassword = new String(txtConfirmPassword.getPassword());
            Role role = (Role) cbRole.getSelectedItem();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
                NotificationUtil.error(frame, "Lỗi", "Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            if (!UserValidator.isValidEmail(email)) {
                NotificationUtil.error(frame, "Lỗi", "Email không hợp lệ!");
                return;
            }

            if (!password.equals(confirmPassword)) {
                NotificationUtil.error(frame, "Lỗi", "Mật khẩu nhập lại không khớp!");
                return;
            }

            if (!UserValidator.isValidPassword(password)) {
                NotificationUtil.error(frame, "Lỗi", "Password cần ít nhất 6 ký tự, gồm chữ và số!");
                return;
            }

            String result = controller.registerGUI(username, password, fullName, email, role);
            if (result.toLowerCase().contains("thành công") || result.toLowerCase().contains("success")) {
                NotificationUtil.success(frame, "Thành công", "Đăng ký thành công!\nVui lòng đăng nhập lại.");
                txtUsername.setText("");
                txtFullName.setText("");
                txtEmail.setText("");
                txtPassword.setText("");
                txtConfirmPassword.setText("");
                frame.showLogin();
            } else {
                NotificationUtil.warning(frame, "Thông báo lỗi", result);
            }
        });

        btnBack.addActionListener(e -> frame.showLogin());

        btnPanel.add(btnRegister);
        btnPanel.add(btnBack);

        add(btnPanel, BorderLayout.SOUTH);
    }
}
