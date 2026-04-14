package userauth.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import userauth.controller.AuctionController;
import userauth.model.AuctionItem;
import userauth.model.User;

import java.util.List;

public class SellerPanel extends BorderPane {
    private final AuthFrame frame;
    private final AuctionController auctionController;
    private User currentUser;

    private final TableView<AuctionItem> table;
    private final TextField txtName;
    private final TextArea txtDesc;
    private final TextField txtPrice;
    private final TextField txtCategory;
    private final Spinner<Integer> spinDuration;
    private final Button btnCreate;

    private int editingId = -1;

    public SellerPanel(AuthFrame frame, AuctionController auctionController) {
        this.frame = frame;
        this.auctionController = auctionController;

        UITheme.stylePage(this);
        setTop(UITheme.createHero(
                "TRUNG TAM NGUOI BAN",
                "Tao, cap nhat va quan ly cac phien dau gia cua ban theo thoi gian thuc."
        ));
        BorderPane.setMargin(getTop(), new Insets(0, 0, 14, 0));

        BorderPane formSection = UITheme.createSection("TAO PHIEN DAU GIA");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        txtName = new TextField();
        txtCategory = new TextField();
        txtDesc = new TextArea();
        txtPrice = new TextField();
        spinDuration = new Spinner<>(1, 99999, 30);
        btnCreate = new Button("TAO MOI");
        Button btnClear = new Button("HUY CAP NHAT");

        UITheme.styleTextField(txtName);
        UITheme.styleTextField(txtCategory);
        UITheme.styleTextArea(txtDesc);
        UITheme.styleTextField(txtPrice);
        UITheme.styleSpinner(spinDuration);
        UITheme.styleSuccessButton(btnCreate);
        UITheme.styleSecondaryButton(btnClear);

        txtName.setPromptText("Nhap ten san pham");
        txtCategory.setPromptText("Vi du: Dien tu");
        txtDesc.setPromptText("Mo ta ngan cho san pham");
        txtPrice.setPromptText("Gia khoi diem");

        form.add(UITheme.createFieldLabel("Ten san pham"), 0, 0);
        form.add(txtName, 1, 0);
        form.add(UITheme.createFieldLabel("Danh muc"), 2, 0);
        form.add(txtCategory, 3, 0);

        form.add(UITheme.createFieldLabel("Mo ta"), 0, 1);
        form.add(txtDesc, 1, 1);
        form.add(UITheme.createFieldLabel("Gia khoi diem"), 2, 1);
        form.add(txtPrice, 3, 1);

        form.add(UITheme.createFieldLabel("Thoi gian dau gia (phut)"), 0, 2);
        form.add(spinDuration, 1, 2);

        HBox formActions = new HBox(10, btnCreate, btnClear);
        form.add(formActions, 0, 3, 4, 1);

        GridPane.setHgrow(txtName, Priority.ALWAYS);
        GridPane.setHgrow(txtCategory, Priority.ALWAYS);
        GridPane.setHgrow(txtDesc, Priority.ALWAYS);
        GridPane.setHgrow(txtPrice, Priority.ALWAYS);
        formSection.setCenter(form);

        table = new TableView<>();
        UITheme.styleTable(table);
        buildColumns();

        BorderPane tableSection = UITheme.createSection("PHIEN DAU GIA CUA TOI");
        tableSection.setCenter(table);

        Button btnEdit = new Button("SUA DONG CHON");
        Button btnDelete = new Button("XOA / HUY PHIEN");
        Button btnCloseAuction = new Button("KET THUC SOM");
        Button btnLogout = new Button("DANG XUAT");
        UITheme.stylePrimaryButton(btnEdit);
        UITheme.styleSecondaryButton(btnDelete);
        UITheme.styleSecondaryButton(btnCloseAuction);
        UITheme.styleGhostButton(btnLogout);

        HBox bottomPanel = new HBox(10, btnEdit, btnDelete, btnCloseAuction, btnLogout);

        VBox body = new VBox(12, formSection, tableSection, bottomPanel);
        VBox.setVgrow(tableSection, Priority.ALWAYS);
        setCenter(body);

        btnCreate.setOnAction(event -> saveAuction());
        btnClear.setOnAction(event -> resetForm());
        btnEdit.setOnAction(event -> loadSelectedAuctionForEdit());
        btnDelete.setOnAction(event -> deleteSelectedAuction());
        btnCloseAuction.setOnAction(event -> closeSelectedAuction());
        btnLogout.setOnAction(event -> {
            currentUser = null;
            frame.showLogin();
        });
    }

    public void setUser(User user) {
        this.currentUser = user;
        resetForm();
    }

    public void refreshData() {
        if (currentUser == null) {
            return;
        }
        List<AuctionItem> myAuctions = auctionController.getAuctionsBySeller(currentUser.getId());
        table.setItems(FXCollections.observableArrayList(myAuctions));
    }

    private void saveAuction() {
        try {
            String name = txtName.getText().trim();
            String desc = txtDesc.getText().trim();
            String cat = txtCategory.getText().trim();
            double price = Double.parseDouble(txtPrice.getText().trim());
            int durationMinutes = spinDuration.getValue();
            long start = System.currentTimeMillis();
            long end = start + (long) durationMinutes * 60 * 1000;

            String result = editingId == -1
                    ? auctionController.createAuction(name, desc, price, start, end, cat, currentUser.getId())
                    : auctionController.updateAuction(editingId, currentUser.getId(), name, desc, price, start, end, cat);

            if ("SUCCESS".equals(result)) {
                NotificationUtil.success(frame.getWindow(), "THONG BAO", "Luu phien dau gia thanh cong.");
                resetForm();
                refreshData();
            } else {
                NotificationUtil.error(frame.getWindow(), "LOI", result);
            }
        } catch (NumberFormatException ex) {
            NotificationUtil.error(frame.getWindow(), "LOI", "Gia khoi diem khong hop le.");
        }
    }

    private void loadSelectedAuctionForEdit() {
        AuctionItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(frame.getWindow(), "THONG BAO", "Hay chon mot phien dau gia.");
            return;
        }

        editingId = item.getId();
        txtName.setText(item.getName());
        txtCategory.setText(item.getCategory());
        txtPrice.setText(String.valueOf(item.getStartPrice()));
        txtDesc.setText(item.getDescription());

        long durationMs = item.getEndTime() - item.getStartTime();
        int durationMin = (int) Math.max(1, durationMs / 60000);
        spinDuration.getValueFactory().setValue(durationMin);
        btnCreate.setText("LUU CAP NHAT");
    }

    private void deleteSelectedAuction() {
        AuctionItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(frame.getWindow(), "THONG BAO", "Hay chon mot phien dau gia.");
            return;
        }
        boolean confirm = NotificationUtil.confirm(frame.getWindow(), "XAC NHAN", "Ban co chac muon xoa hoac huy phien nay?");
        if (!confirm) {
            return;
        }
        String result = auctionController.deleteAuction(item.getId(), currentUser.getId());
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(frame.getWindow(), "THONG BAO", "Da xoa hoac huy phien thanh cong.");
            refreshData();
        } else {
            NotificationUtil.error(frame.getWindow(), "LOI", result);
        }
    }

    private void closeSelectedAuction() {
        AuctionItem item = table.getSelectionModel().getSelectedItem();
        if (item == null) {
            NotificationUtil.warning(frame.getWindow(), "THONG BAO", "Hay chon mot phien dau gia.");
            return;
        }
        String result = auctionController.closeAuction(item.getId(), currentUser.getId());
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(frame.getWindow(), "THONG BAO", "Da ket thuc phien dau gia.");
            refreshData();
        } else {
            NotificationUtil.error(frame.getWindow(), "LOI", result);
        }
    }

    private void resetForm() {
        editingId = -1;
        txtName.clear();
        txtDesc.clear();
        txtPrice.clear();
        txtCategory.clear();
        spinDuration.getValueFactory().setValue(30);
        btnCreate.setText("TAO MOI");
    }

    private void buildColumns() {
        TableColumn<AuctionItem, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        idColumn.setPrefWidth(70);

        TableColumn<AuctionItem, String> nameColumn = new TableColumn<>("Ten SP");
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getName()));

        TableColumn<AuctionItem, String> categoryColumn = new TableColumn<>("Danh muc");
        categoryColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));

        TableColumn<AuctionItem, String> startPriceColumn = new TableColumn<>("Gia khoi diem");
        startPriceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(UITheme.formatMoney(data.getValue().getStartPrice())));

        TableColumn<AuctionItem, String> currentBidColumn = new TableColumn<>("Gia hien tai");
        currentBidColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(UITheme.formatMoney(data.getValue().getCurrentHighestBid())));

        TableColumn<AuctionItem, String> statusColumn = new TableColumn<>("Trang thai");
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus().name()));

        TableColumn<AuctionItem, String> durationColumn = new TableColumn<>("Thoi gian");
        durationColumn.setCellValueFactory(data -> {
            long minutes = Math.max(1, (data.getValue().getEndTime() - data.getValue().getStartTime()) / 60000);
            return new ReadOnlyStringWrapper(minutes + " phut");
        });

        TableColumn<AuctionItem, String> remainingColumn = new TableColumn<>("Con lai");
        remainingColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatRemaining(data.getValue())));

        table.getColumns().addAll(
                idColumn,
                nameColumn,
                categoryColumn,
                startPriceColumn,
                currentBidColumn,
                statusColumn,
                durationColumn,
                remainingColumn
        );
    }

    private String formatRemaining(AuctionItem item) {
        long remainMs = item.getEndTime() - System.currentTimeMillis();
        if (remainMs <= 0) {
            return "Het gio";
        }
        long remainMin = remainMs / 60000;
        long remainSec = (remainMs % 60000) / 1000;
        return remainMin + " phut " + remainSec + "s";
    }
}
