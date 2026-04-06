package userauth.gui;

import userauth.controller.AuthController;
import userauth.model.User;
import userauth.exception.UnauthorizedException;

import javax.swing.*;
import javax.swing.border.Border;
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
        setBackground(Color.WHITE);

        // Header
        JLabel lblTitle = new JLabel("ĐĂNG NHẬP", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitle.setForeground(new Color(41, 128, 185));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(40, 0, 40, 0));
        add(lblTitle, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Border fieldBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(153, 204, 255), 2, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8));

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(new Font("Arial", Font.BOLD, 14));
        lblUser.setForeground(Color.DARK_GRAY);
        formPanel.add(lblUser, gbc);

        gbc.gridy = 1;
        txtUsername = new JTextField(20);
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 14));
        txtUsername.setPreferredSize(new Dimension(200, 35));
        txtUsername.setBackground(new Color(245, 250, 255)); // Màu xanh nhạt rất sáng
        txtUsername.setForeground(Color.BLACK);
        txtUsername.setBorder(fieldBorder);
        formPanel.add(txtUsername, gbc);

        gbc.gridy = 2;
        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(new Font("Arial", Font.BOLD, 14));
        lblPass.setForeground(Color.DARK_GRAY);
        formPanel.add(lblPass, gbc);

        gbc.gridy = 3;
        txtPassword = new JPasswordField(20);
        txtPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPassword.setPreferredSize(new Dimension(200, 35));
        txtPassword.setBackground(new Color(245, 250, 255)); // Màu xanh nhạt rất sáng
        txtPassword.setForeground(Color.BLACK);
        txtPassword.setBorder(fieldBorder);
        formPanel.add(txtPassword, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 40, 0));

        JButton btnLogin = new JButton("Đăng Nhập");
        btnLogin.setFont(new Font("Arial", Font.BOLD, 14));
        btnLogin.setBackground(new Color(35, 110, 253)); // Xanh đậm thuần khiết (Primary Deep Blue)
        btnLogin.setForeground(Color.BLACK);
        btnLogin.setPreferredSize(new Dimension(140, 40));
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnRegister = new JButton("Tạo tài khoản");
        btnRegister.setFont(new Font("Arial", Font.PLAIN, 14));
        btnRegister.setContentAreaFilled(false);
        btnRegister.setBorderPainted(false);
        btnRegister.setForeground(Color.BLUE);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnLogin.addActionListener(e -> {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            try {
                User user = controller.login(username, password);
                JOptionPane.showMessageDialog(frame, "Đăng nhập thành công!\nXin chào " + user.getRole(), "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                txtUsername.setText("");
                txtPassword.setText("");
                frame.showRoleDashboard(user);
            } catch (UnauthorizedException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
