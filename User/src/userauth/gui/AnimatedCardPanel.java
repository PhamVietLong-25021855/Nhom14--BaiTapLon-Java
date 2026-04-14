package userauth.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class AnimatedCardPanel extends StackPane {
    private final ObservableMap<String, Node> cards = FXCollections.observableHashMap();
    private String currentCard;

    public void addCard(Node node, String name) {
        if (node == null || name == null || name.isBlank()) {
            return;
        }
        node.setVisible(false);
        node.setManaged(false);
        node.setOpacity(0);
        cards.put(name, node);
        getChildren().add(node);

        if (currentCard == null) {
            currentCard = name;
            node.setManaged(true);
            node.setVisible(true);
            node.setOpacity(1);
        }
    }

    public void showCard(String name) {
        if (name == null || name.equals(currentCard)) {
            return;
        }

        Node next = cards.get(name);
        if (next == null) {
            return;
        }

        Node current = currentCard == null ? null : cards.get(currentCard);
        currentCard = name;

        if (current == null) {
            next.setManaged(true);
            next.setVisible(true);
            next.setOpacity(1);
            return;
        }

        next.toFront();
        next.setManaged(true);
        next.setVisible(true);
        next.setOpacity(0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(170), current);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), next);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition transition = new ParallelTransition(fadeOut, fadeIn);
        transition.setOnFinished(event -> {
            current.setVisible(false);
            current.setManaged(false);
            current.setOpacity(1);
        });
        transition.play();
    }
}
