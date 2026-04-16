package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HomeViewController {
    private static final Duration REVEAL_DURATION = Duration.millis(620);

    @FXML
    private HBox topBar;

    @FXML
    private VBox heroPanel;

    @FXML
    private VBox featurePanel;

    @FXML
    private HBox statsRow;

    @FXML
    private Circle orbOne;

    @FXML
    private Circle orbTwo;

    @FXML
    private Circle orbThree;

    private Runnable showLoginHandler = () -> {};
    private Runnable showRegisterHandler = () -> {};
    private final List<Animation> ambientAnimations = new ArrayList<>();
    private Animation introAnimation;
    private long animationTicket;

    @FXML
    private void initialize() {
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }

    @FXML
    private void handleShowLogin() {
        showLoginHandler.run();
    }

    @FXML
    private void handleShowRegister() {
        showRegisterHandler.run();
    }

    public void setShowLoginHandler(Runnable showLoginHandler) {
        this.showLoginHandler = Objects.requireNonNullElse(showLoginHandler, () -> {});
    }

    public void setShowRegisterHandler(Runnable showRegisterHandler) {
        this.showRegisterHandler = Objects.requireNonNullElse(showRegisterHandler, () -> {});
    }

    public void activate() {
        stopAnimations();
        long ticket = ++animationTicket;
        resetIntroState();
        Platform.runLater(() -> {
            if (ticket != animationTicket) {
                return;
            }
            startAmbientMotion();
            playIntroAnimation();
        });
    }

    public void deactivate() {
        animationTicket++;
        stopAnimations();
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }

    private void playIntroAnimation() {
        introAnimation = new ParallelTransition(
                createReveal(topBar, 0, -30, 40),
                createReveal(heroPanel, -54, 0, 120),
                createReveal(featurePanel, 58, 0, 210),
                createReveal(statsRow, 0, 28, 320)
        );
        introAnimation.play();
    }

    private Animation createReveal(Node node, double fromX, double fromY, double delayMillis) {
        FadeTransition fade = new FadeTransition(REVEAL_DURATION, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_BOTH);
        fade.setDelay(Duration.millis(delayMillis));

        TranslateTransition slide = new TranslateTransition(REVEAL_DURATION, node);
        slide.setFromX(fromX);
        slide.setFromY(fromY);
        slide.setToX(0);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_BOTH);
        slide.setDelay(Duration.millis(delayMillis));

        ScaleTransition scale = new ScaleTransition(REVEAL_DURATION, node);
        scale.setFromX(0.96);
        scale.setFromY(0.96);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_BOTH);
        scale.setDelay(Duration.millis(delayMillis));

        return new ParallelTransition(fade, slide, scale);
    }

    private void startAmbientMotion() {
        ambientAnimations.add(createFloatAnimation(orbOne, 18, -14, 7200));
        ambientAnimations.add(createFloatAnimation(orbTwo, -22, 18, 8400));
        ambientAnimations.add(createFloatAnimation(orbThree, 15, 22, 6800));
        ambientAnimations.add(createPulseAnimation(orbTwo, 1.08, 9600));
        ambientAnimations.add(createPulseAnimation(orbThree, 1.12, 7600));

        for (Animation animation : ambientAnimations) {
            animation.play();
        }
    }

    private Animation createFloatAnimation(Node node, double toX, double toY, int durationMillis) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(durationMillis), node);
        transition.setFromX(0);
        transition.setFromY(0);
        transition.setToX(toX);
        transition.setToY(toY);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.setAutoReverse(true);
        transition.setCycleCount(Animation.INDEFINITE);
        return transition;
    }

    private Animation createPulseAnimation(Node node, double toScale, int durationMillis) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(durationMillis), node);
        transition.setFromX(1);
        transition.setFromY(1);
        transition.setToX(toScale);
        transition.setToY(toScale);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.setAutoReverse(true);
        transition.setCycleCount(Animation.INDEFINITE);
        return transition;
    }

    private void stopAnimations() {
        if (introAnimation != null) {
            introAnimation.stop();
            introAnimation = null;
        }
        for (Animation animation : ambientAnimations) {
            animation.stop();
        }
        ambientAnimations.clear();
    }

    private void resetIntroState() {
        prepareReveal(topBar, 0, -30);
        prepareReveal(heroPanel, -54, 0);
        prepareReveal(featurePanel, 58, 0);
        prepareReveal(statsRow, 0, 28);
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }

    private void showStaticState() {
        showNode(topBar);
        showNode(heroPanel);
        showNode(featurePanel);
        showNode(statsRow);
    }

    private void prepareReveal(Node node, double translateX, double translateY) {
        node.setOpacity(0);
        node.setTranslateX(translateX);
        node.setTranslateY(translateY);
        node.setScaleX(0.96);
        node.setScaleY(0.96);
    }

    private void showNode(Node node) {
        node.setOpacity(1);
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
    }

    private void resetOrb(Node node) {
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
    }
}
