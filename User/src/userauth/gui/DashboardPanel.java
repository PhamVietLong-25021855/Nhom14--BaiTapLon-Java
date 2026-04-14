package userauth.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import userauth.controller.AuthController;
import userauth.model.User;

import java.util.List;

public class DashboardPanel extends BorderPane {
    private final AuthFrame frame;
    private final AuthController controller;
    private final TableView<User> table;
    private User currentUser;

    public DashboardPanel(AuthFrame frame, AuthController controller) {
        this.frame = frame;
        this.controller = controller;

        UITheme.stylePage(this);
        setTop(UITheme.createHero(
                "BANG DIEU KHIEN QUAN TRI",
                "Quan ly tai khoan nguoi dung va kiem soat trang thai he thong."
        ));
        BorderPane.setMargin(getTop(), new Insets(0, 0, 14, 0));

        table = new TableView<>();
        UITheme.styleTable(table);
        buildColumns();

        BorderPane tableSection = UITheme.createSection("DANH SACH TAI KHOAN");
        tableSection.setCenter(table);

        Button btnToggleStatus = new Button("KHOA / MO KHOA");
        Button btnChangePass = new Button("DOI MAT KHAU");
        Button btnLogout = new Button("DANG XUAT");
        UITheme.stylePrimaryButton(btnToggleStatus);
        UITheme.styleSecondaryButton(btnChangePass);
        UITheme.styleGhostButton(btnLogout);

        HBox actions = new HBox(10, btnToggleStatus, btnChangePass, btnLogout);

        VBox body = new VBox(14, tableSection, actions);
        VBox.setVgrow(tableSection, Priority.ALWAYS);
        setCenter(body);

        btnToggleStatus.setOnAction(event -> toggleStatus());
        btnChangePass.setOnAction(event -> {
            if (currentUser != null) {
                frame.showChangePasswordDialog(currentUser);
            }
        });
        btnLogout.setOnAction(event -> frame.showLogin());
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void refreshData() {
        try {
            List<User> users = controller.getAllUsersList();
            table.setItems(FXCollections.observableArrayList(users));
        } catch (Exception ex) {
            NotificationUtil.error(frame.getWindow(), "LOI", "Khong the tai danh sach nguoi dung.");
        }
    }

    private void toggleStatus() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationUtil.warning(frame.getWindow(), "THONG BAO", "Hay chon mot tai khoan.");
            return;
        }
        String result = controller.toggleUserStatus(currentUser.getUsername(), selected.getId());
        if ("SUCCESS".equals(result)) {
            NotificationUtil.success(frame.getWindow(), "THANH CONG", "Cap nhat trang thai thanh cong.");
            refreshData();
        } else {
            NotificationUtil.error(frame.getWindow(), "LOI", result);
        }
    }

    private void buildColumns() {
        TableColumn<User, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        idColumn.setPrefWidth(80);

        TableColumn<User, String> usernameColumn = new TableColumn<>("Ten dang nhap");
        usernameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getUsername()));

        TableColumn<User, String> fullNameColumn = new TableColumn<>("Ho ten");
        fullNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFullName()));

        TableColumn<User, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEmail()));

        TableColumn<User, String> roleColumn = new TableColumn<>("Vai tro");
        roleColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRoleName()));

        TableColumn<User, String> statusColumn = new TableColumn<>("Trang thai");
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStatus()));

        table.getColumns().addAll(idColumn, usernameColumn, fullNameColumn, emailColumn, roleColumn, statusColumn);
    }
}
