package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop AppShellController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class AppShellController {
    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho content host.
    private StackPane contentHost;
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set content.
    public void setContent(Parent content) {
        setContent(content, false);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set content.
    public void setContent(Parent content, boolean animated) {
        if (content == null) {
            contentHost.getChildren().clear();
            return;
        }

        if (contentHost.getChildren().isEmpty()) {
            resetNode(content);
            contentHost.getChildren().setAll(content);
            return;
        }

        Parent current = (Parent) contentHost.getChildren().getLast();
        if (current == content) {
            return;
        }

        resetNode(current);
        contentHost.getChildren().setAll(content);
        if (animated) {
            UiEffects.playEntrance(content, 0, 0, 10);
            return;
        }
        resetNode(content);
    }
    // Phuong thuc: thuc hien chuc nang reset node trong lop AppShellController.
    private void resetNode(Parent node) {
        node.setOpacity(1);
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
    }
}
