package userauth.gui.fxml;

import javafx.scene.Parent;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao record LoadedView; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
record LoadedView<T>(Parent root, T controller) {
}
