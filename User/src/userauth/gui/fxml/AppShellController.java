package userauth.gui.fxml;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class AppShellController {
    @FXML
    private StackPane contentHost;

    public void setContent(Parent content) {
        setContent(content, false);
    }

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

    private void resetNode(Parent node) {
        node.setOpacity(1);
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
    }
}
