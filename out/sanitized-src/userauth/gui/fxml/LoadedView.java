package userauth.gui.fxml;

import javafx.scene.Parent;

// File note: Wrapper giữ cặp root/controller sau khi load FXML.
record LoadedView<T>(Parent root, T controller) {
}

