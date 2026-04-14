package userauth.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Optional;

public final class NotificationUtil {
    private NotificationUtil() {
    }

    public static void success(Window owner, String title, String message) {
        show(Alert.AlertType.INFORMATION, owner, title, message, UITheme.SUCCESS);
    }

    public static void info(Window owner, String title, String message) {
        show(Alert.AlertType.INFORMATION, owner, title, message, UITheme.PRIMARY);
    }

    public static void warning(Window owner, String title, String message) {
        show(Alert.AlertType.WARNING, owner, title, message, UITheme.WARNING);
    }

    public static void error(Window owner, String title, String message) {
        show(Alert.AlertType.ERROR, owner, title, message, UITheme.DANGER);
    }

    public static boolean confirm(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        configureDialog(alert.getDialogPane(), title, message, UITheme.WARNING);
        initOwner(alert, owner);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static String input(Window owner, String title, String message, String defaultValue) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField input = new TextField(defaultValue == null ? "" : defaultValue);
        UITheme.styleTextField(input);

        Label text = new Label(message);
        text.setWrapText(true);
        text.setTextFill(UITheme.TEXT_SECONDARY);
        text.setFont(UITheme.bodyFont());

        VBox content = new VBox(12, text, input);
        content.setPadding(new Insets(0));
        configureDialog(pane, title, content, UITheme.PRIMARY);

        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        UITheme.stylePrimaryButton(okButton);
        UITheme.styleGhostButton(cancelButton);

        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? input.getText().trim() : null);
        return dialog.showAndWait().orElse(null);
    }

    private static void show(Alert.AlertType type, Window owner, String title, String message, Color accent) {
        Alert alert = new Alert(type);
        configureDialog(alert.getDialogPane(), title, message, accent);
        initOwner(alert, owner);
        alert.showAndWait();
    }

    private static void initOwner(Dialog<?> dialog, Window owner) {
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
    }

    private static void configureDialog(DialogPane pane, String title, String message, Color accent) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setTextFill(UITheme.TEXT_SECONDARY);
        label.setFont(UITheme.bodyFont());
        configureDialog(pane, title, label, accent);
    }

    private static void configureDialog(DialogPane pane, String title, VBox content, Color accent) {
        pane.setHeader(null);
        pane.setContent(content);
        pane.setGraphic(null);
        pane.setPadding(new Insets(18));
        pane.setBackground(UITheme.createSection("").getBackground());
        pane.setBorder(UITheme.createSection("").getBorder());

        Label titleLabel = new Label(title);
        titleLabel.setFont(UITheme.sectionTitleFont());
        titleLabel.setTextFill(UITheme.TEXT_PRIMARY);

        VBox wrapper = new VBox(14, titleLabel, content);
        wrapper.setPadding(new Insets(6, 4, 6, 4));
        pane.setContent(wrapper);
        pane.setStyle(
                "-fx-background-color: " + UITheme.toRgb(UITheme.CARD_BG) + ";" +
                "-fx-border-color: " + UITheme.toRgb(accent) + ";" +
                "-fx-border-radius: 18;" +
                "-fx-background-radius: 18;"
        );
        pane.lookupButton(ButtonType.OK);
        pane.lookupButton(ButtonType.CANCEL);
        styleButtons(pane);
    }

    private static void configureDialog(DialogPane pane, String title, javafx.scene.Node content, Color accent) {
        pane.setHeader(null);
        pane.setGraphic(null);

        Label titleLabel = new Label(title);
        titleLabel.setFont(UITheme.sectionTitleFont());
        titleLabel.setTextFill(UITheme.TEXT_PRIMARY);

        VBox wrapper = new VBox(14, titleLabel, content);
        wrapper.setPadding(new Insets(6, 4, 6, 4));
        pane.setContent(wrapper);
        pane.setStyle(
                "-fx-background-color: " + UITheme.toRgb(UITheme.CARD_BG) + ";" +
                "-fx-border-color: " + UITheme.toRgb(accent) + ";" +
                "-fx-border-radius: 18;" +
                "-fx-background-radius: 18;"
        );
        styleButtons(pane);
    }

    private static void styleButtons(DialogPane pane) {
        pane.getButtonTypes().forEach(buttonType -> {
            Button button = (Button) pane.lookupButton(buttonType);
            if (button == null) {
                return;
            }
            ButtonBar.ButtonData data = buttonType.getButtonData();
            if (data == ButtonBar.ButtonData.OK_DONE || data == ButtonBar.ButtonData.YES) {
                UITheme.stylePrimaryButton(button);
            } else {
                UITheme.styleGhostButton(button);
            }
        });
    }
}
