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
        setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("HỆ THỐNG QUẢN TRỊ ADMIN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(lblTitle, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[] { "ID", "Username", "Họ Tên", "Email", "Role", "Trạng Thái" }, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        
        JButton btnToggleStatus = new JButton("Khóa/Mở Khóa Tài Khoản");
        JButton btnChangePass = new JButton("Đổi Mật Khẩu");
        JButton btnLogout = new JButton("Đăng Xuất");

        btnToggleStatus.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(frame, "Hãy chọn một tài khoản.");
                return;
            }
            int targetId = (int) tableModel.getValueAt(row, 0);
            String result = controller.toggleUserStatus(currentUser.getUsername(), targetId);
            if (result.equals("SUCCESS")) {
                JOptionPane.showMessageDialog(frame, "Cập nhật trạng thái thành công!");
                refreshData();
            } else {
                JOptionPane.showMessageDialog(frame, result, "Lỗi", JOptionPane.ERROR_MESSAGE);
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
