package userauth.gui;

import userauth.controller.AuthController;
import userauth.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DashboardPanel extends JPanel {
    private AuthFrame frame;
    private AuthController controller;
    private JTable table;
    private DefaultTableModel tableModel;
    private User currentUser;

    public DashboardPanel(AuthFrame frame, AuthController controller) {
        this.frame = frame;
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(UITheme.APP_BG);

        JLabel lblTitle = new JLabel("HỆ THỐNG QUẢN TRỊ ADMIN", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.sectionTitleFont());
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(16, 0, 10, 0));
        add(lblTitle, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[] { "ID", "Username", "Họ Tên", "Email", "Role", "Trạng Thái" }, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JPanel tableSection = UITheme.createRoundedSection("Danh sách tài khoản", new BorderLayout());
        tableSection.setBorder(BorderFactory.createCompoundBorder(
                tableSection.getBorder(),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)
        ));
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tableSection.add(scrollPane, BorderLayout.CENTER);
        add(tableSection, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(UITheme.APP_BG);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 16, 10));
        
        JButton btnToggleStatus = new JButton("Khóa/Mở Khóa Tài Khoản");
        JButton btnChangePass = new JButton("Đổi Mật Khẩu");
        JButton btnLogout = new JButton("Đăng Xuất");
        UITheme.stylePrimaryButton(btnToggleStatus);
        UITheme.styleSecondaryButton(btnChangePass);
        UITheme.styleGhostButton(btnLogout);

        btnToggleStatus.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                NotificationUtil.warning(frame, "Thông báo", "Hãy chọn một tài khoản.");
                return;
            }
            int targetId = (int) tableModel.getValueAt(row, 0);
            String result = controller.toggleUserStatus(currentUser.getUsername(), targetId);
            if (result.equals("SUCCESS")) {
                NotificationUtil.success(frame, "Thành công", "Cập nhật trạng thái thành công!");
                refreshData();
            } else {
                NotificationUtil.error(frame, "Lỗi", result);
            }
        });

        btnChangePass.addActionListener(e -> {
            if (currentUser != null) frame.showChangePasswordDialog(currentUser);
        });

        btnLogout.addActionListener(e -> frame.showLogin());

        bottomPanel.add(btnToggleStatus);
        bottomPanel.add(btnChangePass);
        bottomPanel.add(btnLogout);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void refreshData() {
        tableModel.setRowCount(0);
        try {
            List<User> users = controller.getAllUsersList();
            for (User u : users) {
                tableModel.addRow(new Object[] { u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(), u.getStatus() });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
