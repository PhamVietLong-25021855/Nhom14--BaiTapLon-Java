package userauth.gui;

import userauth.controller.AuctionController;
import userauth.model.AuctionItem;
import userauth.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SellerPanel extends JPanel {
    private AuthFrame frame;
    private AuctionController auctionController;
    private User currentUser;

    private JTable table;
    private DefaultTableModel tableModel;

    private JTextField txtName, txtDesc, txtPrice, txtCategory;
    private JSpinner spinDuration; // Thời gian đấu giá tính bằng phút
    private JButton btnCreate, btnClear;
    
    private int editingId = -1; // -1 means Create Mode

    public SellerPanel(AuthFrame frame, AuctionController auctionController) {
        this.frame = frame;
        this.auctionController = auctionController;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header
        JLabel lblTitle = new JLabel("BẢNG ĐIỀU KHIỂN NGƯỜI BÁN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(lblTitle, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());

        // --- CREATE / EDIT FORM ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin Phiên Đấu Giá"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Tên:"), gbc);
        txtName = new JTextField(15); gbc.gridx = 1; formPanel.add(txtName, gbc);

        gbc.gridx = 2; gbc.gridy = 0; formPanel.add(new JLabel("Danh mục:"), gbc);
        txtCategory = new JTextField(15); gbc.gridx = 3; formPanel.add(txtCategory, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Mô tả:"), gbc);
        txtDesc = new JTextField(15); gbc.gridx = 1; formPanel.add(txtDesc, gbc);

        gbc.gridx = 2; gbc.gridy = 1; formPanel.add(new JLabel("Giá khởi điểm:"), gbc);
        txtPrice = new JTextField(15); gbc.gridx = 3; formPanel.add(txtPrice, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Thời gian đấu giá (phút):"), gbc);
        spinDuration = new JSpinner(new SpinnerNumberModel(30, 1, 99999, 1)); // Mặc định 30 phút, tối thiểu 1 phút
        gbc.gridx = 1; formPanel.add(spinDuration, gbc);

        JPanel formBtnBox = new JPanel();
        btnCreate = new JButton("Tạo Mới / Lưu");
        btnCreate.setBackground(new Color(46, 204, 113));
        btnClear = new JButton("Hủy Cập Nhật");
        formBtnBox.add(btnCreate);
        formBtnBox.add(btnClear);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        formPanel.add(formBtnBox, gbc);

        centerPanel.add(formPanel, BorderLayout.NORTH);

        // --- MANAGE AUCTIONS TABLE ---
        tableModel = new DefaultTableModel(new String[]{"ID", "Tên Sp", "Danh mục", "Giá K.Điểm", "Giá Hiện Tại", "Trạng thái", "Thời gian (phút)", "Còn lại"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Sản Phẩm Của Tôi"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // --- BOTTOM PANEL ---
        JPanel bottomPanel = new JPanel();
        JButton btnEdit = new JButton("Sửa Dòng Chọn");
        JButton btnDelete = new JButton("Xóa/Hủy Dòng Chọn");
        JButton btnCloseAuction = new JButton("Kết Thúc Sớm");
        
        JButton btnLogout = new JButton("Đăng Xuất");

        bottomPanel.add(btnEdit);
        bottomPanel.add(btnDelete);
        bottomPanel.add(btnCloseAuction);
        bottomPanel.add(btnLogout);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- ACTION LISTENERS ---
        btnCreate.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                String desc = txtDesc.getText().trim();
                String cat = txtCategory.getText().trim();
                double price = Double.parseDouble(txtPrice.getText().trim());
                int durationMinutes = (int) spinDuration.getValue();
                long start = System.currentTimeMillis();
                long end = start + (long) durationMinutes * 60 * 1000;

                String res;
                if (editingId == -1) {
                    res = auctionController.createAuction(name, desc, price, start, end, cat, currentUser.getId());
                } else {
                    res = auctionController.updateAuction(editingId, currentUser.getId(), name, desc, price, start, end, cat);
                }

                if (res.equals("SUCCESS")) {
                    JOptionPane.showMessageDialog(frame, "Thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    resetForm();
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(frame, res, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Đơn giá không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnClear.addActionListener(e -> resetForm());

        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            editingId = (int) tableModel.getValueAt(row, 0);
            txtName.setText((String) tableModel.getValueAt(row, 1));
            txtCategory.setText((String) tableModel.getValueAt(row, 2));
            txtPrice.setText(String.valueOf(tableModel.getValueAt(row, 3)));
            
            AuctionItem item = auctionController.getAllAuctions().stream().filter(a -> a.getId() == editingId).findFirst().orElse(null);
            if (item != null) {
                txtDesc.setText(item.getDescription());
                // Tính thời gian đấu giá ban đầu (phút)
                long durationMs = item.getEndTime() - item.getStartTime();
                int durationMin = (int)(durationMs / 60000);
                if (durationMin < 1) durationMin = 1;
                spinDuration.setValue(durationMin);
            }
            btnCreate.setText("Lưu Cập Nhật");
        });

        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int id = (int) tableModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(frame, "Bạn có chắc chắn muốn xóa/hủy phiên này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String rep = auctionController.deleteAuction(id, currentUser.getId());
                if (rep.equals("SUCCESS")) {
                    JOptionPane.showMessageDialog(frame, "Đã xóa/hủy thành công.");
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(frame, rep, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnCloseAuction.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int auctionId = (int) tableModel.getValueAt(row, 0);
            String r = auctionController.closeAuction(auctionId, currentUser.getId());
            if (r.equals("SUCCESS")) {
                JOptionPane.showMessageDialog(frame, "Đã kết thúc phiên!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            } else {
                JOptionPane.showMessageDialog(frame, r, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnLogout.addActionListener(e -> {
            currentUser = null;
            frame.showLogin();
        });
    }

    private void resetForm() {
        editingId = -1;
        txtName.setText(""); txtDesc.setText(""); txtPrice.setText(""); txtCategory.setText("");
        spinDuration.setValue(30); // Mặc định 30 phút
        btnCreate.setText("Tạo Mới");
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void refreshData() {
        if (currentUser == null) return;
        tableModel.setRowCount(0);
        long now = System.currentTimeMillis();
        List<AuctionItem> myAuctions = auctionController.getAuctionsBySeller(currentUser.getId());
        for (AuctionItem a : myAuctions) {
            // Tính tổng thời gian đấu giá (phút)
            long totalMin = (a.getEndTime() - a.getStartTime()) / 60000;
            // Tính thời gian còn lại
            String remaining;
            long remainMs = a.getEndTime() - now;
            if (remainMs <= 0) {
                remaining = "Hết giờ";
            } else {
                long remainMin = remainMs / 60000;
                long remainSec = (remainMs % 60000) / 1000;
                remaining = remainMin + " phút " + remainSec + "s";
            }
            tableModel.addRow(new Object[] { 
                a.getId(), a.getName(), a.getCategory(), 
                a.getStartPrice(), a.getCurrentHighestBid(),
                a.getStatus().name(),
                totalMin + " phút",
                remaining
            });
        }
    }
}
