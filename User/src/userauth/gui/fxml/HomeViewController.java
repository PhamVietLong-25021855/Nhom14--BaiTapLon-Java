package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.controller.HomepageController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.HomepageAnnouncement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeViewController {
    private static final Duration REVEAL_DURATION = Duration.millis(620);
    private static final int MAX_DISPLAY_AUCTIONS = 6;
    private static final int MAX_DISPLAY_ANNOUNCEMENTS = 6;

    @FXML
    private HBox topBar;

    @FXML
    private ScrollPane landingScrollPane;

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

    @FXML
    private Label statRunningValue;

    @FXML
    private Label statUpcomingValue;

    @FXML
    private Label statAnnouncementValue;

    @FXML
    private VBox upcomingAuctionsContainer;

    @FXML
    private VBox announcementsContainer;

    @FXML
    private VBox feedSection;

    @FXML
    private VBox auctionColumn;

    @FXML
    private VBox announcementColumn;

    private Runnable showLoginHandler = () -> {};
    private Runnable showRegisterHandler = () -> {};
    private final List<Animation> ambientAnimations = new ArrayList<>();
    private final List<ScrollRevealTarget> scrollRevealTargets = new ArrayList<>();
    private final Map<Node, Double> scrollRevealProgress = new HashMap<>();
    private final Timeline refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(2), event -> refreshContent())
    );

    private Animation introAnimation;
    private long animationTicket;
    private AuctionController auctionController;
    private HomepageController homepageController;

    @FXML
    private void initialize() {
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        configureScrollRevealTargets();
        registerScrollRevealListeners();
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

    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }

    public void setHomepageController(HomepageController homepageController) {
        this.homepageController = homepageController;
    }

    public void activate() {
        stopAnimations();
        refreshContent();
        if (refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }

        long ticket = ++animationTicket;
        resetIntroState();
        resetScrollRevealState();
        Platform.runLater(() -> {
            if (ticket != animationTicket) {
                return;
            }
            updateScrollReveal();
            startAmbientMotion();
            playIntroAnimation();
        });
    }

    public void deactivate() {
        animationTicket++;
        refreshTimeline.stop();
        stopAnimations();
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }

    public void refreshContent() {
        List<AuctionItem> auctions = auctionController == null ? List.of() : auctionController.getAllAuctions();
        List<HomepageAnnouncement> announcements = homepageController == null ? List.of() : homepageController.getAllAnnouncements();
        Map<Integer, AuctionItem> auctionLookup = new HashMap<>();

        int runningCount = 0;
        int upcomingCount = 0;
        for (AuctionItem auction : auctions) {
            auctionLookup.put(auction.getId(), auction);
            if (auction.getStatus() == AuctionStatus.RUNNING) {
                runningCount++;
            } else if (auction.getStatus() == AuctionStatus.OPEN) {
                upcomingCount++;
            }
        }

        statRunningValue.setText(String.valueOf(runningCount));
        statUpcomingValue.setText(String.valueOf(upcomingCount));
        statAnnouncementValue.setText(String.valueOf(announcements.size()));

        renderAuctionCards(auctions);
        renderAnnouncementCards(announcements, auctionLookup);
        requestScrollRevealUpdate();
    }

    private void renderAuctionCards(List<AuctionItem> auctions) {
        upcomingAuctionsContainer.getChildren().clear();

        List<AuctionItem> displayAuctions = auctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.RUNNING || item.getStatus() == AuctionStatus.OPEN)
                .sorted(Comparator
                        .comparingInt((AuctionItem item) -> item.getStatus() == AuctionStatus.RUNNING ? 0 : 1)
                        .thenComparingLong(AuctionItem::getStartTime))
                .limit(MAX_DISPLAY_AUCTIONS)
                .toList();

        if (displayAuctions.isEmpty()) {
            upcomingAuctionsContainer.getChildren().add(createEmptyCard(
                    "Chua co phien hien thi",
                    "Khi seller tao phien moi hoac phien bat dau, danh sach nay se tu dong cap nhat."
            ));
            return;
        }

        for (AuctionItem auction : displayAuctions) {
            upcomingAuctionsContainer.getChildren().add(createAuctionCard(auction));
        }
    }

    private void renderAnnouncementCards(List<HomepageAnnouncement> announcements, Map<Integer, AuctionItem> auctionLookup) {
        announcementsContainer.getChildren().clear();

        List<HomepageAnnouncement> displayAnnouncements = announcements.stream()
                .limit(MAX_DISPLAY_ANNOUNCEMENTS)
                .toList();

        if (displayAnnouncements.isEmpty()) {
            announcementsContainer.getChildren().add(createEmptyCard(
                    "Admin chua dang bai",
                    "Bai thong bao, lich dau gia va gioi thieu vat pham se xuat hien tai day sau khi admin cap nhat."
            ));
            return;
        }

        for (HomepageAnnouncement announcement : displayAnnouncements) {
            announcementsContainer.getChildren().add(createAnnouncementCard(announcement, auctionLookup.get(announcement.getLinkedAuctionId())));
        }
    }

    private VBox createAuctionCard(AuctionItem auction) {
        VBox card = new VBox(10);
        card.getStyleClass().add("landing-feed-card");

        HBox header = new HBox(10);
        Label title = createLabel(auction.getName(), "feed-card-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);
        Label chip = createStatusChip(
                auction.getStatus() == AuctionStatus.RUNNING ? "DANG DAU GIA" : "SAP MO",
                auction.getStatus() == AuctionStatus.RUNNING ? "status-chip-live" : "status-chip-upcoming"
        );
        header.getChildren().addAll(title, chip);

        Label category = createLabel("Danh muc: " + safeValue(auction.getCategory()), "feed-card-meta");
        Label schedule = createLabel("Lich: " + AuctionViewFormatter.formatScheduleRange(auction), "feed-card-body");
        schedule.setWrapText(true);
        Label highestBid = createLabel("Gia hien tai: " + AuctionViewFormatter.formatMoney(auction.getCurrentHighestBid()), "feed-card-body");
        Label timeInfo = createLabel(
                auction.getStatus() == AuctionStatus.RUNNING
                        ? "Con lai: " + AuctionViewFormatter.formatTimeLeft(auction)
                        : AuctionViewFormatter.formatTimeLeft(auction),
                "feed-card-foot"
        );
        timeInfo.setWrapText(true);

        card.getChildren().addAll(header, category, schedule, highestBid, timeInfo);
        return card;
    }

    private VBox createAnnouncementCard(HomepageAnnouncement announcement, AuctionItem linkedAuction) {
        VBox card = new VBox(10);
        card.getStyleClass().add("landing-feed-card");

        HBox header = new HBox(10);
        Label title = createLabel(announcement.getTitle(), "feed-card-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);
        Label chip = createStatusChip("ADMIN", "status-chip-admin");
        header.getChildren().addAll(title, chip);

        Label summary = createLabel(announcement.getSummary(), "feed-card-body");
        summary.setWrapText(true);
        Label schedule = createLabel("Lich dang: " + announcement.getScheduleText(), "feed-card-body");
        schedule.setWrapText(true);

        VBox linkedAuctionBox = new VBox(4);
        if (linkedAuction != null) {
            linkedAuctionBox.getStyleClass().add("linked-auction-box");
            Label linkedTitle = createLabel("Vat pham lien ket: " + linkedAuction.getName(), "feed-card-meta");
            linkedTitle.setWrapText(true);
            Label linkedSchedule = createLabel("Khung gio: " + AuctionViewFormatter.formatScheduleRange(linkedAuction), "feed-card-foot");
            linkedSchedule.setWrapText(true);
            linkedAuctionBox.getChildren().addAll(linkedTitle, linkedSchedule);
        }

        if (!safeValue(announcement.getDetails()).isEmpty()) {
            Label details = createLabel(announcement.getDetails(), "feed-card-foot");
            details.setWrapText(true);
            card.getChildren().add(details);
        }

        Label updatedAt = createLabel("Cap nhat luc: " + AuctionViewFormatter.formatDateTime(announcement.getUpdatedAt()), "feed-card-foot");
        updatedAt.setWrapText(true);

        card.getChildren().addAll(0, List.of(header, summary, schedule));
        if (!linkedAuctionBox.getChildren().isEmpty()) {
            card.getChildren().add(linkedAuctionBox);
        }
        card.getChildren().add(updatedAt);
        return card;
    }

    private VBox createEmptyCard(String titleText, String bodyText) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("landing-feed-card", "landing-feed-empty-card");

        Label title = createLabel(titleText, "feed-card-title");
        Label body = createLabel(bodyText, "feed-card-foot");
        body.setWrapText(true);

        card.getChildren().addAll(title, body);
        return card;
    }

    private Label createStatusChip(String text, String extraStyleClass) {
        Label chip = new Label(text);
        chip.getStyleClass().add("status-chip");
        chip.getStyleClass().add(extraStyleClass);
        return chip;
    }

    private Label createLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
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
        showNode(feedSection);
        showNode(auctionColumn);
        showNode(announcementColumn);
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

    private void configureScrollRevealTargets() {
        scrollRevealTargets.clear();
        scrollRevealTargets.add(new ScrollRevealTarget(feedSection, 0, 84, 0.92, 0.0, 0.84, 0.38));
        scrollRevealTargets.add(new ScrollRevealTarget(auctionColumn, -46, 30, 0.96, 0.0, 0.82, 0.30));
        scrollRevealTargets.add(new ScrollRevealTarget(announcementColumn, 46, 30, 0.96, 0.0, 0.76, 0.30));
    }

    private void registerScrollRevealListeners() {
        landingScrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> updateScrollReveal());
        landingScrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> updateScrollReveal());
        landingScrollPane.contentProperty().addListener((observable, oldValue, newValue) -> requestScrollRevealUpdate());
    }

    private void resetScrollRevealState() {
        scrollRevealProgress.clear();
        for (ScrollRevealTarget target : scrollRevealTargets) {
            applyScrollReveal(target, 0);
        }
    }

    private void requestScrollRevealUpdate() {
        Platform.runLater(this::updateScrollReveal);
    }

    private void updateScrollReveal() {
        if (landingScrollPane == null || landingScrollPane.getScene() == null || landingScrollPane.getContent() == null) {
            return;
        }

        Node content = landingScrollPane.getContent();
        double viewportHeight = landingScrollPane.getViewportBounds().getHeight();
        double contentHeight = content.getLayoutBounds().getHeight();
        if (viewportHeight <= 0 || contentHeight <= 0) {
            return;
        }

        double maxScroll = Math.max(0, contentHeight - viewportHeight);
        double scrollTop = maxScroll * landingScrollPane.getVvalue();

        for (ScrollRevealTarget target : scrollRevealTargets) {
            double progress = computeScrollRevealProgress(target, content, scrollTop, viewportHeight);
            double currentProgress = Math.max(scrollRevealProgress.getOrDefault(target.node(), 0.0), progress);
            scrollRevealProgress.put(target.node(), currentProgress);
            applyScrollReveal(target, currentProgress);
        }
    }

    private double computeScrollRevealProgress(
            ScrollRevealTarget target,
            Node content,
            double scrollTop,
            double viewportHeight
    ) {
        double nodeTop = content.sceneToLocal(target.node().localToScene(target.node().getBoundsInLocal())).getMinY();
        double revealDistance = Math.max(1, viewportHeight * target.distanceRatio());
        double triggerLine = scrollTop + (viewportHeight * target.triggerRatio());
        double revealStart = triggerLine + revealDistance;
        return clamp((revealStart - nodeTop) / revealDistance);
    }

    private void applyScrollReveal(ScrollRevealTarget target, double progress) {
        double eased = ease(progress);
        Node node = target.node();
        node.setOpacity(target.minOpacity() + ((1 - target.minOpacity()) * eased));
        node.setTranslateX(target.fromX() * (1 - eased));
        node.setTranslateY(target.fromY() * (1 - eased));

        double scale = target.fromScale() + ((1 - target.fromScale()) * eased);
        node.setScaleX(scale);
        node.setScaleY(scale);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private double ease(double progress) {
        return progress * progress * (3 - (2 * progress));
    }

    private record ScrollRevealTarget(
            Node node,
            double fromX,
            double fromY,
            double fromScale,
            double minOpacity,
            double triggerRatio,
            double distanceRatio
    ) {
    }
}
