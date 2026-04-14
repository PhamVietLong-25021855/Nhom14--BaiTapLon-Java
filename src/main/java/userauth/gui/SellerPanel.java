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
    private JSpinner spinDuration;
    private JButton btnCreate, btnClear;
    
    private int editingId = -1;

    public SellerPanel(AuthFrame frame, AuctionController auctionController) {
        this.frame = frame;
        this.auctionController = auctionController;
        setLayout(new BorderLayout());
        setBackground(UITheme.APP_BG);

        JLabel lblTitle = new JLabel("BẢNG ĐIỀU KHIỂN NGƯỜI BÁN", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.sectionTitleFont());
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(14, 0, 4, 0));

        JLabel lblHint = new JLabel("Tạo, chỉnh sửa và quản lý phiên đấu giá một cách trực quan", SwingConstants.CENTER);
        lblHint.setFont(UITheme.bodyFont());
        lblHint.setForeground(Color.BLACK);
        lblHint.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.APP_BG);
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblHint, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(UITheme.APP_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

        JPanel formSection = UITheme.createRoundedSection("Thông tin phiên đấu giá", new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(createFormLabel("Tên sản phẩm"), gbc);
        txtName = new JTextField(15); gbc.gridx = 1; formPanel.add(txtName, gbc);

        gbc.gridx = 2; gbc.gridy = 0; formPanel.add(createFormLabel("Danh mục"), gbc);
        txtCategory = new JTextField(15); gbc.gridx = 3; formPanel.add(txtCategory, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(createFormLabel("Mô tả"), gbc);
        txtDesc = new JTextField(15); gbc.gridx = 1; formPanel.add(txtDesc, gbc);

        gbc.gridx = 2; gbc.gridy = 1; formPanel.add(createFormLabel("Giá khởi điểm"), gbc);
        txtPrice = new JTextField(15); gbc.gridx = 3; formPanel.add(txtPrice, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(createFormLabel("Thời gian đấu giá (phút)"), gbc);
        spinDuration = new JSpinner(new SpinnerNumberModel(30, 1, 99999, 1));
        gbc.gridx = 1; formPanel.add(spinDuration, gbc);
        UITheme.styleTextField(txtName);
        UITheme.styleTextField(txtCategory);
        UITheme.styleTextField(txtDesc);
        UITheme.styleTextField(txtPrice);
        ((JSpinner.DefaultEditor) spinDuration.getEditor()).getTextField().setHorizontalAlignment(JTextField.LEFT);
        UITheme.styleTextField(((JSpinner.DefaultEditor) spinDuration.getEditor()).getTextField());

        JPanel formBtnBox = new JPanel();
        formBtnBox.setOpaque(false);
        btnCreate = new JButton("Tạo Mới / Lưu");
        UITheme.styleSuccessButton(btnCreate);
        btnClear = new JButton("Hủy Cập Nhật");
        UITheme.styleSecondaryButton(btnClear);
        formBtnBox.add(btnCreate);
        formBtnBox.add(btnClear);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        gbc.insets = new Insets(10, 6, 2, 6);
        formPanel.add(formBtnBox, gbc);
        formSection.add(formPanel, BorderLayout.CENTER);
        centerPanel.add(formSection, BorderLayout.NORTH);

        JPanel tableSection = UITheme.createRoundedSection("Phiên đấu giá của tôi", new BorderLayout());
        tableModel = new DefaultTableModel(new String[]{"ID", "Tên Sp", "Danh mục", "Giá K.Điểm", "Giá Hiện Tại", "Trạng thái", "Thời gian (phút)", "Còn lại"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.CARD_BG);
        tableSection.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(tableSection, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(UITheme.APP_BG);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));
        JButton btnEdit = new JButton("Sửa Dòng Chọn");
        JButton btnDelete = new JButton("Xóa/Hủy Dòng Chọn");
        JButton btnCloseAuction = new JButton("Kết Thúc Sớm");
        
        JButton btnLogout = new JButton("Đăng Xuất");
        UITheme.stylePrimaryButton(btnEdit);
        UITheme.styleSecondaryButton(btnDelete);
        UITheme.styleSecondaryButton(btnCloseAuction);
        UITheme.styleGhostButton(btnLogout);

        bottomPanel.add(btnEdit);
        bottomPanel.add(btnDelete);
        bottomPanel.add(btnCloseAuction);
        bottomPanel.add(btnLogout);
        add(bottomPanel, BorderLayout.SOUTH);

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
                    NotificationUtil.success(frame, "Thông báo", "Thành công!");
                    resetForm();
                    refreshData();
                } else {
                    NotificationUtil.error(frame, "Lỗi", res);
                }
            } catch (NumberFormatException ex) {
                NotificationUtil.error(frame, "Lỗi", "Đơn giá không hợp lệ!");
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
            boolean confirm = NotificationUtil.confirm(frame, "Xác nhận", "Bạn có chắc chắn muốn xóa/hủy phiên này?");
            if (confirm) {
                String rep = auctionController.deleteAuction(id, currentUser.getId());
                if (rep.equals("SUCCESS")) {
                    NotificationUtil.success(frame, "Thông báo", "Đã xóa/hủy thành công.");
                    refreshData();
                } else {
                    NotificationUtil.error(frame, "Lỗi", rep);
                }
            }
        });

        btnCloseAuction.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int auctionId = (int) tableModel.getValueAt(row, 0);
            String r = auctionController.closeAuction(auctionId, currentUser.getId());
            if (r.equals("SUCCESS")) {
                NotificationUtil.success(frame, "Thông báo", "Đã kết thúc phiên!");
                refreshData();
            } else {
                NotificationUtil.error(frame, "Lỗi", r);
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
        spinDuration.setValue(30);
        btnCreate.setText("Tạo Mới");
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text + ":");
        label.setFont(UITheme.labelFont());
        label.setForeground(Color.BLACK);
        return label;
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
            long totalMin = (a.getEndTime() - a.getStartTime()) / 60000;
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
