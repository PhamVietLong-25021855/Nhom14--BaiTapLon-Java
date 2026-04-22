package userauth.gui.fxml;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NotificationUtil {
    private static final double TOAST_WIDTH = 360;
    private static final double TOAST_GAP = 14;
    private static final double TOAST_MARGIN = 24;
    private static final Duration TOAST_DURATION = Duration.seconds(3.6);
    private static final Duration TOAST_HIDE_DURATION = Duration.millis(150);
    private static final String TOAST_HIDING_KEY = "toast.hiding";

    private static final Map<Window, List<Popup>> ACTIVE_TOASTS = new HashMap<>();

    private NotificationUtil() {
    }

    public static void success(Window owner, String title, String message) {
        showToast(owner, UiText.text(title), UiText.text(message), "toast-success");
    }

    public static void info(Window owner, String title, String message) {
        showToast(owner, UiText.text(title), UiText.text(message), "toast-info");
    }

    public static void warning(Window owner, String title, String message) {
        showToast(owner, UiText.text(title), UiText.text(message), "toast-warning");
    }

    public static void error(Window owner, String title, String message) {
        showToast(owner, UiText.text(title), UiText.text(message), "toast-error");
    }

    public static boolean confirm(Window owner, String title, String message) {
        LoadedView<ModalMessageController> view = FxmlRuntime.loadView(NotificationUtil.class, "modal-message.fxml", "dialog");
        String localizedTitle = UiText.text(title);
        String localizedMessage = UiText.text(message);
        Stage dialog = FxmlRuntime.createModalDialog(owner, localizedTitle, view.root(), 420, 240);
        view.controller().setDialogStage(dialog);
        view.controller().configure(localizedTitle, localizedMessage, UiText.text("CONFIRM"), "primary-button", UiText.text("CANCEL"), true);
        dialog.showAndWait();
        return view.controller().isConfirmed();
    }

    public static String input(Window owner, String title, String message, String defaultValue) {
        LoadedView<TextInputDialogController> view = FxmlRuntime.loadView(NotificationUtil.class, "text-input-dialog.fxml", "dialog");
        String localizedTitle = UiText.text(title);
        String localizedMessage = UiText.text(message);
        Stage dialog = FxmlRuntime.createModalDialog(owner, localizedTitle, view.root(), 430, 280);
        view.controller().setDialogStage(dialog);
        view.controller().configure(
                localizedTitle,
                localizedMessage,
                defaultValue,
                UiText.text("CONFIRM"),
                UiText.text("CANCEL"),
                "primary-button"
        );
        dialog.setOnShown(event -> view.controller().requestInputFocus());
        dialog.showAndWait();
        return view.controller().getInputValue();
    }

    private static void showMessage(Window owner, String title, String message, String primaryText, String primaryStyleClass) {
        LoadedView<ModalMessageController> view = FxmlRuntime.loadView(NotificationUtil.class, "modal-message.fxml", "dialog");
        String localizedTitle = UiText.text(title);
        String localizedMessage = UiText.text(message);
        Stage dialog = FxmlRuntime.createModalDialog(owner, localizedTitle, view.root(), 420, 240);
        view.controller().setDialogStage(dialog);
        view.controller().configure(localizedTitle, localizedMessage, UiText.text(primaryText), primaryStyleClass, null, false);
        dialog.showAndWait();
    }

    private static void showToast(Window owner, String title, String message, String toneStyleClass) {
        Window resolvedOwner = resolveOwner(owner);
        if (resolvedOwner == null) {
            showMessage(owner, title, message, "CLOSE", "primary-button");
            return;
        }

        LoadedView<ToastNotificationController> view = FxmlRuntime.loadView(NotificationUtil.class, "toast-notification.fxml", "toast");
        view.controller().configure(title, message, toneStyleClass);

        Parent root = view.root();
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(false);
        popup.getContent().add(root);

        List<Popup> toasts = ACTIVE_TOASTS.computeIfAbsent(resolvedOwner, ignored -> new ArrayList<>());
        toasts.add(popup);

        positionToasts(resolvedOwner);
        popup.show(resolvedOwner);
        positionToasts(resolvedOwner);

        root.setOpacity(0);
        root.setTranslateX(12);
        UiEffects.playEntrance(root, 0, 12, 0);

        PauseTransition wait = new PauseTransition(TOAST_DURATION);
        wait.setOnFinished(event -> hideToast(resolvedOwner, popup, root));
        wait.play();

        root.setOnMouseClicked(event -> hideToast(resolvedOwner, popup, root));
    }

    private static void hideToast(Window owner, Popup popup, Parent root) {
        if (Boolean.TRUE.equals(root.getProperties().get(TOAST_HIDING_KEY))) {
            return;
        }
        if (!popup.isShowing()) {
            removeToast(owner, popup);
            return;
        }

        root.getProperties().put(TOAST_HIDING_KEY, Boolean.TRUE);
        boolean previousCache = root.isCache();
        CacheHint previousCacheHint = root.getCacheHint();
        root.setCache(true);
        root.setCacheHint(CacheHint.SPEED);

        FadeTransition fade = new FadeTransition(TOAST_HIDE_DURATION, root);
        fade.setFromValue(root.getOpacity());
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition slide = new TranslateTransition(TOAST_HIDE_DURATION, root);
        slide.setFromX(root.getTranslateX());
        slide.setToX(14);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.setOnFinished(event -> {
            root.getProperties().remove(TOAST_HIDING_KEY);
            root.setCache(previousCache);
            root.setCacheHint(previousCacheHint);
            popup.hide();
            removeToast(owner, popup);
        });
        transition.play();
    }

    private static void removeToast(Window owner, Popup popup) {
        popup.hide();
        List<Popup> toasts = ACTIVE_TOASTS.get(owner);
        if (toasts != null) {
            toasts.remove(popup);
            if (toasts.isEmpty()) {
                ACTIVE_TOASTS.remove(owner);
            }
        }
        positionToasts(owner);
    }

    private static void positionToasts(Window owner) {
        List<Popup> toasts = ACTIVE_TOASTS.get(owner);
        if (toasts == null || toasts.isEmpty()) {
            return;
        }

        Rectangle2D bounds = owner == null
                ? Screen.getPrimary().getVisualBounds()
                : new Rectangle2D(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());

        double startX = bounds.getMinX() + bounds.getWidth() - TOAST_WIDTH - TOAST_MARGIN;
        double currentY = bounds.getMinY() + TOAST_MARGIN;

        for (Popup toast : toasts) {
            toast.setX(startX);
            toast.setY(currentY);
            currentY += 96 + TOAST_GAP;
        }
    }

    private static Window resolveOwner(Window owner) {
        if (owner != null) {
            return owner;
        }

        for (Window window : Window.getWindows()) {
            if (window != null && window.isShowing()) {
                return window;
            }
        }
        return null;
    }
}
