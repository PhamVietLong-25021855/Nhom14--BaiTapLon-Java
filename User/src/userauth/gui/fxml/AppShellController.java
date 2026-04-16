package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class AppShellController {
    private static final Duration PAGE_TRANSITION_DURATION = Duration.millis(320);

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
        resetNode(content);

        if (!animated) {
            contentHost.getChildren().setAll(content);
            return;
        }

        content.setOpacity(0);
        content.setTranslateX(56);
        contentHost.getChildren().add(content);

        FadeTransition fadeOut = new FadeTransition(PAGE_TRANSITION_DURATION, current);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition slideOut = new TranslateTransition(PAGE_TRANSITION_DURATION, current);
        slideOut.setFromX(0);
        slideOut.setToX(-56);
        slideOut.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fadeIn = new FadeTransition(PAGE_TRANSITION_DURATION, content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition slideIn = new TranslateTransition(PAGE_TRANSITION_DURATION, content);
        slideIn.setFromX(56);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_BOTH);

        Animation transition = new ParallelTransition(fadeOut, slideOut, fadeIn, slideIn);
        transition.setOnFinished(event -> {
            resetNode(current);
            resetNode(content);
            contentHost.getChildren().setAll(content);
        });
        transition.play();
    }

    private void resetNode(Parent node) {
        node.setOpacity(1);
        node.setTranslateX(0);
    }
}
