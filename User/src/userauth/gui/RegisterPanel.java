package userauth.gui;

import userauth.controller.AuthController;
import userauth.model.Role;
import userauth.validation.UserValidator;

import javax.swing.*;
import javax.swing.border.Border;
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
        setBackground(Color.WHITE);

        // Header
        JLabel lblTitle = new JLabel("ĐĂNG KÝ TÀI KHOẢN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setForeground(new Color(39, 174, 96));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(lblTitle, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 20, 5, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Border fieldBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(153, 204, 255), 2, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8));

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        txtUsername = new JTextField(20);
        txtUsername.setBackground(new Color(245, 250, 255));
        txtUsername.setForeground(Color.BLACK);
        txtUsername.setBorder(fieldBorder);
        gbc.gridy = 1;
        formPanel.add(txtUsername, gbc);

        gbc.gridy = 2;
        formPanel.add(new JLabel("Họ tên:"), gbc);
        txtFullName = new JTextField(20);
        txtFullName.setBackground(new Color(245, 250, 255));
        txtFullName.setForeground(Color.BLACK);
        txtFullName.setBorder(fieldBorder);
        gbc.gridy = 3;
        formPanel.add(txtFullName, gbc);

        gbc.gridy = 4;
        formPanel.add(new JLabel("Email:"), gbc);
        txtEmail = new JTextField(20);
        txtEmail.setBackground(new Color(245, 250, 255));
        txtEmail.setForeground(Color.BLACK);
        txtEmail.setBorder(fieldBorder);
        gbc.gridy = 5;
        formPanel.add(txtEmail, gbc);

        gbc.gridy = 6;
        formPanel.add(new JLabel("Password:"), gbc);
        txtPassword = new JPasswordField(20);
        txtPassword.setBackground(new Color(245, 250, 255));
        txtPassword.setForeground(Color.BLACK);
        txtPassword.setBorder(fieldBorder);
        gbc.gridy = 7;
        formPanel.add(txtPassword, gbc);

        gbc.gridy = 8;
        formPanel.add(new JLabel("Nhập lại Password:"), gbc);
        txtConfirmPassword = new JPasswordField(20);
        txtConfirmPassword.setBackground(new Color(245, 250, 255));
        txtConfirmPassword.setForeground(Color.BLACK);
        txtConfirmPassword.setBorder(fieldBorder);
        gbc.gridy = 9;
        formPanel.add(txtConfirmPassword, gbc);

        gbc.gridy = 10;
        formPanel.add(new JLabel("Vai trò (Role):"), gbc);
        cbRole = new JComboBox<>(new Role[] { Role.BIDDER, Role.SELLER, Role.ADMIN });
        cbRole.setBackground(new Color(245, 250, 255));
        cbRole.setForeground(Color.BLACK);
        gbc.gridy = 11;
        formPanel.add(cbRole, gbc);

        add(formPanel, BorderLayout.CENTER);
        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        JButton btnRegister = new JButton("Đăng Ký");
        btnRegister.setBackground(new Color(46, 204, 113)); // Sáng hơn một chút
        btnRegister.setForeground(Color.BLACK);
        btnRegister.setPreferredSize(new Dimension(120, 35));
        btnRegister.setFocusPainted(false);

        JButton btnBack = new JButton("Quay lại Đăng Nhập");
        btnBack.setContentAreaFilled(false);
        btnBack.setBorderPainted(false);
        btnBack.setForeground(Color.BLUE);
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnRegister.addActionListener(e -> {
            String username = txtUsername.getText().trim();
            String fullName = txtFullName.getText().trim();
            String email = txtEmail.getText().trim();
            String password = new String(txtPassword.getPassword());
            String confirmPassword = new String(txtConfirmPassword.getPassword());
            Role role = (Role) cbRole.getSelectedItem();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Vui lòng nhập đầy đủ thông tin!", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!UserValidator.isValidEmail(email)) {
                JOptionPane.showMessageDialog(frame, "Email không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(frame, "Mật khẩu nhập lại không khớp!", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!UserValidator.isValidPassword(password)) {
                JOptionPane.showMessageDialog(frame, "Password cần ít nhất 6 ký tự, gồm chữ và số!", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String result = controller.registerGUI(username, password, fullName, email, role);
            if (result.toLowerCase().contains("thành công") || result.toLowerCase().contains("success")) {
                JOptionPane.showMessageDialog(frame, "Đăng ký thành công!\nVui lòng đăng nhập lại.", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                txtUsername.setText("");
                txtFullName.setText("");
                txtEmail.setText("");
                txtPassword.setText("");
                txtConfirmPassword.setText("");
                frame.showLogin();
            } else {
                JOptionPane.showMessageDialog(frame, result, "Thông báo lỗi", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnBack.addActionListener(e -> frame.showLogin());

        btnPanel.add(btnRegister);
        btnPanel.add(btnBack);

        add(btnPanel, BorderLayout.SOUTH);
    }
}
