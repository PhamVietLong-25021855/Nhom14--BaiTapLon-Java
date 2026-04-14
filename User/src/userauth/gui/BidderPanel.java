package userauth.gui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import userauth.model.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BidderPanel extends BorderPane {
    private static final DateTimeFormatter BID_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuthFrame frame;
    private final AuctionController auctionController;
    private User currentUser;

    private final TableView<AuctionItem> table;
    private final Timeline timeline;

    public BidderPanel(AuthFrame frame, AuctionController auctionController) {
        this.frame = frame;
        this.auctionController = auctionController;

        UITheme.stylePage(this);
        setTop(UITheme.createHero(
                "TRUNG TAM DAU GIA",
                "Theo doi phien dau gia, dat gia nhanh va xem lich su giao dich theo thoi gian thuc."
        ));
        BorderPane.setMargin(getTop(), new Insets(0, 0, 14, 0));

        table = new TableView<>();
        UITheme.styleTable(table);
        buildColumns();

        BorderPane tableSection = UITheme.createSection("DANH SACH PHIEN DAU GIA");
        tableSection.setCenter(table);

        Button btnBid = new Button("DAT GIA");
        Button btnHistory = new Button("XEM LICH SU BID");
        Button btnProfile = new Button("DOI MAT KHAU");
        Button btnLogout = new Button("DANG XUAT");
        UITheme.stylePrimaryButton(btnBid);
        UITheme.styleSecondaryButton(btnHistory);
        UITheme.styleSecondaryButton(btnProfile);
        UITheme.styleGhostButton(btnLogout);

        HBox actions = new HBox(10, btnBid, btnHistory, btnProfile, btnLogout);
        VBox body = new VBox(14, tableSection, actions);
        VBox.setVgrow(tableSection, Priority.ALWAYS);
        setCenter(body);

        btnBid.setOnAction(event -> placeBid());
        btnHistory.setOnAction(event -> showHistory());
        btnProfile.setOnAction(event -> {
            if (currentUser != null) {
                frame.showChangePasswordDialog(currentUser);
            }
        });
        btnLogout.setOnAction(event -> {
            currentUser = null;
            deactivate();
            frame.showLogin();
        });

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshData()));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void activate() {
        if (timeline.getStatus() != Animation.Status.RUNNING) {
            timeline.play();
        }
    }

    public void deactivate() {
        timeline.stop();
    }

    public void refreshData() {
        if (currentUser == null) {
            return;
        }

        AuctionItem selected = table.getSelectionModel().getSelectedItem();
        int selectedId = selected == null ? -1 : selected.getId();

        List<AuctionItem> allAuctions = auctionController.getAllAuctions();
        table.setItems(FXCollections.observableArrayList(allAuctions));

        if (selectedId < 0) {
            return;
        }
        table.getItems().stream()
                .filter(item -> item.getId() == selectedId)
                .findFirst()
                .ifPresent(item -> table.getSelectionModel().select(item));
    }

    private void placeBid() {
        AuctionItem selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(frame.getWindow(), "THONG BAO", "Hay chon mot phien dau gia.");
            return;
        }

        String bidStr = NotificationUtil.input(
                frame.getWindow(),
                "DAT GIA",
                "Gia hien tai: " + UITheme.formatMoney(selected.getCurrentHighestBid()) + "\nNhap muc gia cua ban:",
                ""
        );
        if (bidStr == null || bidStr.isBlank()) {
            return;
        }

        try {
            double amount = Double.parseDouble(bidStr);
            String result = auctionController.placeBid(selected.getId(), currentUser.getId(), amount);
            if ("SUCCESS".equals(result)) {
                NotificationUtil.success(frame.getWindow(), "THONG BAO", "Dat gia thanh cong.");
            } else {
                NotificationUtil.error(frame.getWindow(), "LOI", result);
            }
            refreshData();
        } catch (NumberFormatException ex) {
            NotificationUtil.error(frame.getWindow(), "LOI", "So tien khong hop le.");
        }
    }

    private void showHistory() {
        AuctionItem selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(frame.getWindow(), "THONG BAO", "Hay chon mot phien dau gia.");
            return;
        }

        List<BidTransaction> bids = auctionController.getBidsForAuction(selected.getId());
        StringBuilder history = new StringBuilder();
        for (BidTransaction bid : bids) {
            history.append("User ID: ")
                    .append(bid.getBidderId())
                    .append(" | Gia: ")
                    .append(UITheme.formatMoney(bid.getAmount()))
                    .append(" | Luc: ")
                    .append(BID_TIME.format(Instant.ofEpochMilli(bid.getTimestamp()).atZone(ZoneId.systemDefault())))
                    .append(" | Trang thai: ")
                    .append(bid.getStatus())
                    .append('\n');
        }
        if (history.length() == 0) {
            history.append("Chua co giao dich bid nao.");
        }

        Stage dialog = new Stage();
        dialog.initOwner(frame.getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("LICH SU BID");

        TextArea textArea = new TextArea(history.toString());
        textArea.setEditable(false);
        UITheme.styleTextArea(textArea);
        textArea.setPrefSize(620, 340);

        BorderPane root = UITheme.createSection("LICH SU BID");
        root.setCenter(textArea);
        UITheme.stylePage(root);

        dialog.setScene(new javafx.scene.Scene(root, 680, 420));
        dialog.showAndWait();
    }

    private void buildColumns() {
        TableColumn<AuctionItem, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        idColumn.setPrefWidth(70);

        TableColumn<AuctionItem, String> nameColumn = new TableColumn<>("Ten SP");
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));

        TableColumn<AuctionItem, String> categoryColumn = new TableColumn<>("Danh muc");
        categoryColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));

        TableColumn<AuctionItem, String> currentBidColumn = new TableColumn<>("Gia cao nhat");
        currentBidColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(UITheme.formatMoney(data.getValue().getCurrentHighestBid())));

        TableColumn<AuctionItem, String> statusColumn = new TableColumn<>("Trang thai");
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus().name()));

        TableColumn<AuctionItem, String> timeLeftColumn = new TableColumn<>("Con lai");
        timeLeftColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatTimeLeft(data.getValue())));

        table.getColumns().addAll(idColumn, nameColumn, categoryColumn, currentBidColumn, statusColumn, timeLeftColumn);
    }

    private String formatTimeLeft(AuctionItem item) {
        long now = System.currentTimeMillis();
        if ("RUNNING".equals(item.getStatus().name()) || "OPEN".equals(item.getStatus().name())) {
            long diffMs = item.getEndTime() - now;
            if (now < item.getStartTime()) {
                long waitMin = (item.getStartTime() - now) / 60000;
                long waitSec = ((item.getStartTime() - now) % 60000) / 1000;
                return "Chua mo (" + waitMin + " phut " + waitSec + "s)";
            }
            if (diffMs > 0) {
                long min = diffMs / 60000;
                long sec = (diffMs % 60000) / 1000;
                return min + " phut " + sec + "s";
            }
            return "Het gio";
        }
        return "-";
    }
}
