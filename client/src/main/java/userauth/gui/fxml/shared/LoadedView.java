package userauth.gui.fxml.shared;

import javafx.scene.Parent;

public record LoadedView<T>(Parent root, T controller) {
}
