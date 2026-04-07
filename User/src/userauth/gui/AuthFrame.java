package userauth.gui;

import userauth.controller.AuctionController;
import userauth.controller.AuthController;
import userauth.model.User;

import javax.swing.*;
import java.awt.*;

public class AuthFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private AuthController authController;
    private AuctionController auctionController;

    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private DashboardPanel adminDashboard;
    private SellerPanel sellerPanel;
    private BidderPanel bidderPanel;

    public AuthFrame(AuthController authController, AuctionController auctionController) {
        this.authController = authController;
        this.auctionController = auctionController;
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Hệ Thống Đấu Giá Sản Phẩm");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 640);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(UITheme.APP_BG);

        loginPanel = new LoginPanel(this, authController);
        registerPanel = new RegisterPanel(this, authController);
        adminDashboard = new DashboardPanel(this, authController);
        sellerPanel = new SellerPanel(this, auctionController);
        bidderPanel = new BidderPanel(this, auctionController);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(registerPanel, "REGISTER");
        mainPanel.add(adminDashboard, "ADMIN");
        mainPanel.add(sellerPanel, "SELLER");
        mainPanel.add(bidderPanel, "BIDDER");

        add(mainPanel);
    }

    public void showLogin() {
        setSize(560, 640);
        setLocationRelativeTo(null);
        cardLayout.show(mainPanel, "LOGIN");
    }

    public void showRegister() {
        setSize(620, 760);
        setLocationRelativeTo(null);
        cardLayout.show(mainPanel, "REGISTER");
    }

    public void showRoleDashboard(User user) {
        switch(user.getRole()) {
            case ADMIN:
                setSize(900, 620);
                setLocationRelativeTo(null);
                adminDashboard.setUser(user);
                adminDashboard.refreshData();
                cardLayout.show(mainPanel, "ADMIN");
                break;
            case SELLER:
                setSize(1040, 700);
                setLocationRelativeTo(null);
                sellerPanel.setUser(user);
                sellerPanel.refreshData();
                cardLayout.show(mainPanel, "SELLER");
                break;
            case BIDDER:
                setSize(980, 640);
                setLocationRelativeTo(null);
                bidderPanel.setUser(user);
                bidderPanel.refreshData();
                cardLayout.show(mainPanel, "BIDDER");
                break;
        }
    }

    public void showChangePasswordDialog(User user) {
        JPasswordField pfOld = new JPasswordField();
        JPasswordField pfNew = new JPasswordField();
        Object[] message = {
            "Mật khẩu hiện tại:", pfOld,
            "Mật khẩu mới (Tối thiểu 6 ký tự gồm chữ & số):", pfNew
        };
        int option = JOptionPane.showConfirmDialog(this, message, "Đổi Mật Khẩu", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String oldPass = new String(pfOld.getPassword());
            String newPass = new String(pfNew.getPassword());
            if(oldPass.isEmpty() || newPass.isEmpty()) {
                 NotificationUtil.warning(this, "Lỗi", "Vui lòng nhập đầy đủ mật khẩu!");
                 return;
            }
            String result = authController.changePassword(user.getUsername(), oldPass, newPass);
            if ("SUCCESS".equals(result)) {
                NotificationUtil.success(this, "Thông báo", "Đổi mật khẩu thành công!");
            } else {
                NotificationUtil.error(this, "Lỗi", result);
            }
        }
    }
}

