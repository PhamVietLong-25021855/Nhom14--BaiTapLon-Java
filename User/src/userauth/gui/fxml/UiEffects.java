package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.List;

public final class UiEffects {
    private static final String ENTRANCE_KEY = "ui.effects.entrance";
    private static final String PULSE_KEY = "ui.effects.pulse";
    private static final String SHAKE_KEY = "ui.effects.shake";
    private static final Duration ENTRANCE_DURATION = Duration.millis(220);
    private static final Duration PULSE_DURATION = Duration.millis(120);
    private static final Duration SHAKE_DURATION = Duration.millis(58);

    private UiEffects() {
    }

    public static void playEntrance(Node node, double delayMillis, double fromX, double fromY) {
        if (node == null) {
            return;
        }

        stopTrackedAnimation(node, ENTRANCE_KEY);

        boolean previousCache = node.isCache();
        CacheHint previousCacheHint = node.getCacheHint();
        node.setCache(true);
        node.setCacheHint(CacheHint.SPEED);

        double startX = clampOffset(fromX);
        double startY = clampOffset(fromY);
        node.setOpacity(0);
        node.setTranslateX(startX);
        node.setTranslateY(startY);
        node.setScaleX(1);
        node.setScaleY(1);

        FadeTransition fade = new FadeTransition(ENTRANCE_DURATION, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(clampDelay(delayMillis)));
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(ENTRANCE_DURATION, node);
        slide.setFromX(startX);
        slide.setFromY(startY);
        slide.setToX(0);
        slide.setToY(0);
        slide.setDelay(Duration.millis(clampDelay(delayMillis)));
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition transition = new ParallelTransition(fade, slide);
        trackAnimation(node, ENTRANCE_KEY, transition);
        transition.setOnFinished(event -> finishAnimation(node, ENTRANCE_KEY, previousCache, previousCacheHint));
        transition.play();
    }

    public static void playStaggered(List<? extends Node> nodes, double initialDelay, double stepDelay, double fromY) {
        if (nodes == null) {
            return;
        }

        double delay = initialDelay;
        for (Node node : nodes) {
            playEntrance(node, delay, 0, fromY);
            delay += stepDelay;
        }
    }

    public static void pulse(Node node) {
        if (node == null) {
            return;
        }

        stopTrackedAnimation(node, PULSE_KEY);

        boolean previousCache = node.isCache();
        CacheHint previousCacheHint = node.getCacheHint();
        node.setCache(true);
        node.setCacheHint(CacheHint.SPEED);
        node.setScaleX(1);
        node.setScaleY(1);

        ScaleTransition expand = new ScaleTransition(PULSE_DURATION, node);
        expand.setFromX(1);
        expand.setFromY(1);
        expand.setToX(1.02);
        expand.setToY(1.02);
        expand.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(PULSE_DURATION, node);
        settle.setFromX(1.02);
        settle.setFromY(1.02);
        settle.setToX(1);
        settle.setToY(1);
        settle.setInterpolator(Interpolator.EASE_BOTH);

        SequentialTransition transition = new SequentialTransition(expand, settle);
        trackAnimation(node, PULSE_KEY, transition);
        transition.setOnFinished(event -> finishAnimation(node, PULSE_KEY, previousCache, previousCacheHint));
        transition.play();
    }

    public static void shake(Node node) {
        if (node == null) {
            return;
        }

        stopTrackedAnimation(node, SHAKE_KEY);

        boolean previousCache = node.isCache();
        CacheHint previousCacheHint = node.getCacheHint();
        node.setCache(true);
        node.setCacheHint(CacheHint.SPEED);
        node.setTranslateX(0);

        TranslateTransition left = createShakeStep(node, -5);
        TranslateTransition right = createShakeStep(node, 5);
        TranslateTransition settle = createShakeStep(node, 0);

        SequentialTransition transition = new SequentialTransition(left, right, left, settle);
        trackAnimation(node, SHAKE_KEY, transition);
        transition.setOnFinished(event -> finishAnimation(node, SHAKE_KEY, previousCache, previousCacheHint));
        transition.play();
    }

    private static TranslateTransition createShakeStep(Node node, double targetX) {
        TranslateTransition transition = new TranslateTransition(SHAKE_DURATION, node);
        transition.setToX(targetX);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        return transition;
    }

    private static void trackAnimation(Node node, String key, Animation animation) {
        node.getProperties().put(key, animation);
    }

    private static void stopTrackedAnimation(Node node, String key) {
        Object value = node.getProperties().remove(key);
        if (value instanceof Animation animation) {
            animation.stop();
        }
    }

    private static void finishAnimation(Node node, String key, boolean previousCache, CacheHint previousCacheHint) {
        node.getProperties().remove(key);
        node.setOpacity(1);
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
        node.setCache(previousCache);
        node.setCacheHint(previousCacheHint);
    }

    private static double clampOffset(double value) {
        return Math.max(-20, Math.min(20, value));
    }

    private static double clampDelay(double value) {
        return Math.max(0, Math.min(220, value));
    }
}
