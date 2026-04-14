package userauth.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.text.DecimalFormat;

public final class UITheme {
    public static final Color APP_BG = Color.web("#e4eaee");
    public static final Color CARD_BG = Color.web("#f2f6f9");
    public static final Color CARD_ALT = Color.web("#ecf2f6");
    public static final Color PRIMARY = Color.web("#212121");
    public static final Color PRIMARY_HOVER = Color.web("#0a0a0a");
    public static final Color SUCCESS = Color.web("#2e373e");
    public static final Color SUCCESS_HOVER = Color.web("#182026");
    public static final Color WARNING = Color.web("#f59e0b");
    public static final Color DANGER = Color.web("#dc3545");
    public static final Color MUTED = Color.web("#727e88");
    public static final Color INPUT_BG = Color.web("#fafcfd");
    public static final Color TEXT_PRIMARY = Color.web("#101418");
    public static final Color TEXT_SECONDARY = Color.web("#37424d");
    public static final Insets PAGE_PADDING = new Insets(24);

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.##");
    private static final CornerRadii CARD_RADII = new CornerRadii(24);
    private static final CornerRadii INPUT_RADII = new CornerRadii(14);
    private static final CornerRadii BUTTON_RADII = new CornerRadii(14);

    private UITheme() {
    }

    public static Font titleFont() {
        return Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 30);
    }

    public static Font sectionTitleFont() {
        return Font.font("Segoe UI", FontWeight.BOLD, 18);
    }

    public static Font bodyFont() {
        return Font.font("Segoe UI", 14);
    }

    public static Font labelFont() {
        return Font.font("Segoe UI", FontWeight.BOLD, 13);
    }

    public static void stylePage(Pane pane) {
        pane.setPadding(PAGE_PADDING);
        pane.setBackground(new Background(new BackgroundFill(
                new LinearGradient(
                        0,
                        0,
                        1,
                        1,
                        true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, APP_BG),
                        new Stop(1, Color.web("#dde5ea"))
                ),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    public static StackPane createHero(String title, String subtitle) {
        StackPane hero = new StackPane();
        hero.setMinWidth(320);
        hero.setPadding(new Insets(28));
        hero.setBackground(new Background(new BackgroundFill(
                new LinearGradient(
                        0,
                        0,
                        1,
                        1,
                        true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#3f4d57")),
                        new Stop(0.55, Color.web("#2a333a")),
                        new Stop(1, Color.web("#182025"))
                ),
                CARD_RADII,
                Insets.EMPTY
        )));
        hero.setEffect(cardShadow());

        Circle topGlow = new Circle(110, Color.web("#ffffff", 0.10));
        Circle bottomGlow = new Circle(84, Color.web("#9fb4c2", 0.16));
        StackPane.setAlignment(topGlow, Pos.TOP_RIGHT);
        StackPane.setAlignment(bottomGlow, Pos.BOTTOM_LEFT);
        topGlow.setTranslateX(48);
        topGlow.setTranslateY(-34);
        bottomGlow.setTranslateX(-34);
        bottomGlow.setTranslateY(40);

        Label badge = new Label("LIVE AUCTION HUB");
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        badge.setTextFill(Color.web("#e7eef3"));
        badge.setPadding(new Insets(8, 12, 8, 12));
        badge.setBackground(new Background(new BackgroundFill(Color.web("#ffffff", 0.12), new CornerRadii(999), Insets.EMPTY)));
        badge.setBorder(new Border(new BorderStroke(Color.web("#ffffff", 0.12), BorderStrokeStyle.SOLID, new CornerRadii(999), BorderWidths.DEFAULT)));

        Label titleLabel = new Label(title);
        titleLabel.setFont(titleFont());
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setWrapText(true);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(bodyFont());
        subtitleLabel.setTextFill(Color.web("#dce6ee"));
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(420);

        VBox copy = new VBox(14, badge, titleLabel, subtitleLabel);
        copy.setAlignment(Pos.TOP_LEFT);
        copy.setMaxWidth(Double.MAX_VALUE);

        hero.getChildren().addAll(topGlow, bottomGlow, copy);
        StackPane.setAlignment(copy, Pos.CENTER_LEFT);
        return hero;
    }

    public static BorderPane createSection(String title) {
        BorderPane section = new BorderPane();
        decorateCard(section);
        if (title != null && !title.isBlank()) {
            Label sectionTitle = new Label(title);
            sectionTitle.setFont(sectionTitleFont());
            sectionTitle.setTextFill(TEXT_PRIMARY);
            section.setTop(sectionTitle);
            BorderPane.setMargin(sectionTitle, new Insets(0, 0, 16, 0));
        }
        return section;
    }

    public static void decorateCard(Region region) {
        region.setPadding(new Insets(20, 22, 20, 22));
        region.setBackground(new Background(new BackgroundFill(CARD_BG, CARD_RADII, Insets.EMPTY)));
        region.setBorder(new Border(new BorderStroke(
                Color.web("#95a0a9"),
                BorderStrokeStyle.SOLID,
                CARD_RADII,
                new BorderWidths(1.2)
        )));
        region.setEffect(cardShadow());
    }

    public static Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setFont(labelFont());
        label.setTextFill(TEXT_SECONDARY);
        return label;
    }

    public static void styleTextField(TextField field) {
        styleTextInput(field);
    }

    public static void styleTextArea(TextArea area) {
        styleTextInput(area);
        area.setWrapText(true);
        area.setPrefRowCount(3);
    }

    public static void styleTextInput(TextInputControl control) {
        if (control instanceof Region region) {
            region.setBackground(new Background(new BackgroundFill(INPUT_BG, INPUT_RADII, Insets.EMPTY)));
            region.setBorder(new Border(new BorderStroke(MUTED, BorderStrokeStyle.SOLID, INPUT_RADII, new BorderWidths(1.5))));
            region.setPadding(new Insets(10, 12, 10, 12));
        }
        control.setFont(bodyFont());
        control.setStyle("-fx-text-fill: " + toRgb(TEXT_PRIMARY) + "; -fx-highlight-fill: " + toRgb(Color.web("#c9d4dc")) + ";");
    }

    public static void styleComboBox(ComboBox<?> comboBox) {
        styleControlBox(comboBox);
    }

    public static void styleSpinner(Spinner<Integer> spinner) {
        styleControlBox(spinner);
        spinner.setEditable(true);
        styleTextInput(spinner.getEditor());
        spinner.getEditor().setPrefColumnCount(5);
    }

    private static void styleControlBox(Control control) {
        control.setStyle(
                "-fx-background-color: " + toRgb(INPUT_BG) + ";" +
                "-fx-border-color: " + toRgb(MUTED) + ";" +
                "-fx-border-radius: 14;" +
                "-fx-background-radius: 14;" +
                "-fx-padding: 4 8 4 8;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 14px;" +
                "-fx-text-fill: " + toRgb(TEXT_PRIMARY) + ";"
        );
    }

    public static void stylePrimaryButton(Button button) {
        styleFilledButton(button, PRIMARY, PRIMARY_HOVER, Color.WHITE);
    }

    public static void styleSuccessButton(Button button) {
        styleFilledButton(button, SUCCESS, SUCCESS_HOVER, Color.WHITE);
    }

    public static void styleSecondaryButton(Button button) {
        styleFilledButton(button, Color.web("#57626c"), Color.web("#39434c"), Color.WHITE);
    }

    public static void styleGhostButton(Button button) {
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.setTextFill(TEXT_PRIMARY);
        button.setCursor(Cursor.HAND);
        applyGhostStyle(button, TEXT_PRIMARY);
        button.hoverProperty().addListener((obs, oldValue, hovered) ->
                applyGhostStyle(button, hovered ? Color.web("#2f3942") : TEXT_PRIMARY)
        );
    }

    private static void styleFilledButton(Button button, Color base, Color hover, Color text) {
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.setTextFill(text);
        button.setCursor(Cursor.HAND);
        button.setPadding(new Insets(11, 18, 11, 18));
        applyFilledStyle(button, base, text);
        button.hoverProperty().addListener((obs, oldValue, hovered) ->
                applyFilledStyle(button, hovered ? hover : base, text)
        );
    }

    private static void applyFilledStyle(Button button, Color background, Color text) {
        button.setTextFill(text);
        button.setBackground(new Background(new BackgroundFill(background, BUTTON_RADII, Insets.EMPTY)));
        button.setBorder(new Border(new BorderStroke(
                Color.web("#141414", 0.28),
                BorderStrokeStyle.SOLID,
                BUTTON_RADII,
                new BorderWidths(1.1)
        )));
    }

    private static void applyGhostStyle(Button button, Color text) {
        button.setTextFill(text);
        button.setBackground(Background.EMPTY);
        button.setBorder(new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.SOLID, BUTTON_RADII, BorderWidths.DEFAULT)));
        button.setPadding(new Insets(11, 8, 11, 8));
    }

    public static <T> void styleTable(TableView<T> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(38);
        table.setPlaceholder(createEmptyState("Khong co du lieu", "Danh sach se hien thi tai day."));
        table.setBackground(new Background(new BackgroundFill(CARD_BG, new CornerRadii(18), Insets.EMPTY)));
        table.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: transparent;" +
                "-fx-table-cell-border-color: transparent;" +
                "-fx-padding: 0;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 13px;" +
                "-fx-selection-bar: " + toRgb(Color.web("#cfd9e1")) + ";" +
                "-fx-selection-bar-non-focused: " + toRgb(Color.web("#d8e0e6")) + ";"
        );
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: transparent;");
                } else if (isSelected()) {
                    setStyle("-fx-background-color: " + toRgb(Color.web("#cfd9e1")) + ";");
                } else {
                    Color rowColor = getIndex() % 2 == 0 ? CARD_BG : CARD_ALT;
                    setStyle("-fx-background-color: " + toRgb(rowColor) + ";");
                }
            }
        });
    }

    public static Node createEmptyState(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLabel.setTextFill(TEXT_PRIMARY);

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setFont(bodyFont());
        subtitleLabel.setTextFill(TEXT_SECONDARY);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(220);

        VBox box = new VBox(6, titleLabel, subtitleLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        return box;
    }

    public static HBox fillWidth(Node left, Node right) {
        HBox row = new HBox(18, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        if (left instanceof Region leftRegion) {
            leftRegion.setMaxWidth(Double.MAX_VALUE);
        }
        if (right instanceof Region rightRegion) {
            rightRegion.setMaxWidth(Double.MAX_VALUE);
        }
        return row;
    }

    public static String formatMoney(double amount) {
        return MONEY.format(amount);
    }

    public static DropShadow cardShadow() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(22);
        shadow.setOffsetY(10);
        shadow.setColor(Color.web("#1d252c", 0.14));
        return shadow;
    }

    public static String toRgb(Color color) {
        return String.format(
                "rgba(%d, %d, %d, %.3f)",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                color.getOpacity()
        );
    }
}
