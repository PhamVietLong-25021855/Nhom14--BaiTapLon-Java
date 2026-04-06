package userauth.gui;

import userauth.controller.AuctionController;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class BidderPanel extends JPanel {
    private AuthFrame frame;
    private AuctionController auctionController;
    private User currentUser;

    private JTable table;
    private DefaultTableModel tableModel;
    private Timer timer;

    public BidderPanel(AuthFrame frame, AuctionController auctionController) {
        this.frame = frame;
        this.auctionController = auctionController;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("TRUNG TÂM ĐẤU GIÁ (BIDDER)", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        add(lblTitle, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[] { "ID", "Tên Sp", "Danh Mục", "Giá Cao Nhất", "Trạng Thái", "Còn Lại (giây)" }, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        JButton btnBid = new JButton("Đặt Giá Sp Chọn");
        JButton btnHistory = new JButton("Xem Lịch Sử Bid Sp Chọn");
        JButton btnProfile = new JButton("Đổi Mật Khẩu");
        JButton btnLogout = new JButton("Đăng Xuất");

        btnBid.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int auctionId = (int) tableModel.getValueAt(row, 0);
            double currentBid = (double) tableModel.getValueAt(row, 3);
            
            String bidStr = JOptionPane.showInputDialog(frame, "Giá hiện tại: " + currentBid + "\nNhập mức giá của bạn:", "Tham gia đấu giá", JOptionPane.QUESTION_MESSAGE);
            if (bidStr != null && !bidStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(bidStr);
                    String result = auctionController.placeBid(auctionId, currentUser.getId(), amount);
                    if (result.equals("SUCCESS")) JOptionPane.showMessageDialog(frame, "Thành công!");
                    else JOptionPane.showMessageDialog(frame, result, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    refreshData();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnHistory.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int auctionId = (int) tableModel.getValueAt(row, 0);
            List<BidTransaction> bids = auctionController.getBidsForAuction(auctionId);
            StringBuilder sb = new StringBuilder("Lịch sử Bid:\n");
            for(BidTransaction b : bids) {
                sb.append("User ID: ").append(b.getBidderId())
                  .append(" - Giá: ").append(b.getAmount())
                  .append(" - Thời gian: ").append(new java.util.Date(b.getTimestamp())).append("\n");
            }
            JOptionPane.showMessageDialog(frame, sb.toString(), "Chi tiết", JOptionPane.INFORMATION_MESSAGE);
        });

        btnProfile.addActionListener(e -> {
            if (currentUser != null) frame.showChangePasswordDialog(currentUser);
        });

        btnLogout.addActionListener(e -> {
            currentUser = null;
            if(timer != null) timer.stop();
            frame.showLogin();
        });

        bottomPanel.add(btnBid);
        bottomPanel.add(btnHistory);
        bottomPanel.add(btnProfile);
        bottomPanel.add(btnLogout);
        add(bottomPanel, BorderLayout.SOUTH);

        // Timer for real-time update
        timer = new Timer(1000, e -> refreshData());
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    public void refreshData() {
        if (currentUser == null) return;
        
        // Preserve selected row
        int selectedRow = table.getSelectedRow();
        int selectedId = selectedRow >= 0 ? (int) tableModel.getValueAt(selectedRow, 0) : -1;

        tableModel.setRowCount(0);
        long now = System.currentTimeMillis();
        // Lấy tất cả phiên OPEN và RUNNING hoặc FINISHED gần đây để user xem
        List<AuctionItem> all = auctionController.getAllAuctions();
        
        int newSelectedRow = -1;
        int rowIndex = 0;
        
        for (AuctionItem a : all) {
            String timeLeft = "-";
            if (a.getStatus().name().equals("RUNNING") || a.getStatus().name().equals("OPEN")) {
                long diffMs = a.getEndTime() - now;
                if(now < a.getStartTime()) {
                    long waitMin = (a.getStartTime() - now) / 60000;
                    long waitSec = ((a.getStartTime() - now) % 60000) / 1000;
                    timeLeft = "Chưa mở (còn " + waitMin + " phút " + waitSec + "s)";
                } else if(diffMs > 0) {
                    long min = diffMs / 60000;
                    long sec = (diffMs % 60000) / 1000;
                    timeLeft = min + " phút " + sec + "s";
                } else {
                    timeLeft = "Hết giờ";
                }
            }

            tableModel.addRow(new Object[] { 
                a.getId(), a.getName(), a.getCategory(), 
                a.getCurrentHighestBid(), a.getStatus().name(), timeLeft
            });
            
            if (a.getId() == selectedId) {
                newSelectedRow = rowIndex;
            }
            rowIndex++;
        }
        
        if (newSelectedRow >= 0) {
            table.setRowSelectionInterval(newSelectedRow, newSelectedRow);
        }
    }
}
