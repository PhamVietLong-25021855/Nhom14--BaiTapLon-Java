package uet.auctionsystem.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import uet.auctionsystem.controller.AuctionController;
import uet.auctionsystem.controller.HomepageController;
import uet.auctionsystem.model.AuctionItem;
import uet.auctionsystem.model.AuctionStatus;
import uet.auctionsystem.model.BidTransaction;
import uet.auctionsystem.model.HomepageAnnouncement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop HomeViewController; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public class HomeViewController {
    private static final Duration REVEAL_DURATION = Duration.millis(620);
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho auctions.
    private static final int MAX_DISPLAY_AUCTIONS = 6;
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho announcements.
    private static final int MAX_DISPLAY_ANNOUNCEMENTS = 6;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho top bar.
    private HBox topBar;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho landing scroll pane.
    private ScrollPane landingScrollPane;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho hero panel.
    private VBox heroPanel;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho feature panel.
    private VBox featurePanel;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho stats row.
    private HBox statsRow;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho orb one.
    private Circle orbOne;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho orb two.
    private Circle orbTwo;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho orb three.
    private Circle orbThree;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho stat running value.
    private Label statRunningValue;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho stat upcoming value.
    private Label statUpcomingValue;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho stat announcement value.
    private Label statAnnouncementValue;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho upcoming auctions container.
    private VBox upcomingAuctionsContainer;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho announcements container.
    private VBox announcementsContainer;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho feed section.
    private VBox feedSection;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho auction column.
    private VBox auctionColumn;

    @FXML
    // Thuoc tinh: luu trang thai hoac du lieu tam cho announcement column.
    private VBox announcementColumn;

    private Runnable showLoginHandler = () -> {};
    private Runnable showRegisterHandler = () -> {};
    private final List<Animation> ambientAnimations = new ArrayList<>();
    private final List<ScrollRevealTarget> scrollRevealTargets = new ArrayList<>();
    private final Map<Node, Double> scrollRevealProgress = new HashMap<>();
    private final Timeline refreshTimeline = new Timeline(
    // Phuong thuc: thuc hien chuc nang key frame trong lop HomeViewController.
            new KeyFrame(Duration.seconds(5), event -> refreshContent())
    );
    // Thuoc tinh: luu trang thai hoac du lieu tam cho intro animation.
    private Animation introAnimation;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho animation ticket.
    private long animationTicket;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho refresh ticket.
    private long refreshTicket;
    // Thuoc tinh: giu tham chieu den AuctionController de phoi hop xu ly.
    private AuctionController auctionController;
    // Thuoc tinh: giu tham chieu den HomepageController de phoi hop xu ly.
    private HomepageController homepageController;

    @FXML
    // Phuong thuc: khoi dong hoac khoi tao tien trinh initialize.
    private void initialize() {
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle show login.
    private void handleShowLogin() {
        showLoginHandler.run();
    }

    @FXML
    // Phuong thuc: xu ly nghiep vu chinh cho thao tac handle show register.
    private void handleShowRegister() {
        showRegisterHandler.run();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set show login handler.
    public void setShowLoginHandler(Runnable showLoginHandler) {
        this.showLoginHandler = Objects.requireNonNullElse(showLoginHandler, () -> {});
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set show register handler.
    public void setShowRegisterHandler(Runnable showRegisterHandler) {
        this.showRegisterHandler = Objects.requireNonNullElse(showRegisterHandler, () -> {});
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set auction controller.
    public void setAuctionController(AuctionController auctionController) {
        this.auctionController = auctionController;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set homepage controller.
    public void setHomepageController(HomepageController homepageController) {
        this.homepageController = homepageController;
    }
    // Phuong thuc: thuc hien chuc nang activate trong lop HomeViewController.
    public void activate() {
        stopAnimations();
        refreshContent();
        if (refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }
        animationTicket++;
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }
    // Phuong thuc: thuc hien chuc nang deactivate trong lop HomeViewController.
    public void deactivate() {
        animationTicket++;
        refreshTicket++;
        refreshTimeline.stop();
        stopAnimations();
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac refresh content.
    public void refreshContent() {
        long ticket = ++refreshTicket;
        UiAsync.run(
                this::loadHomeSnapshot,
                snapshot -> {
                    if (ticket != refreshTicket) {
                        return;
                    }
                    applyHomeSnapshot(snapshot);
                },
                error -> {
                    if (ticket != refreshTicket) {
                        return;
                    }
                    statRunningValue.setText("0");
                    statUpcomingValue.setText("0");
                    statAnnouncementValue.setText("0");
                }
        );
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac render auction cards.
    private void renderAuctionCards(List<AuctionItem> auctions, Map<Integer, Integer> bidCounts) {
        upcomingAuctionsContainer.getChildren().clear();

        List<AuctionItem> displayAuctions = auctions.stream()
                .filter(item -> item.getStatus() == AuctionStatus.RUNNING || item.getStatus() == AuctionStatus.OPEN)
                .sorted(Comparator
                        .comparingInt((AuctionItem item) -> item.getStatus() == AuctionStatus.RUNNING ? 0 : 1)
                        .thenComparingLong(AuctionItem::getStartTime))
                .limit(MAX_DISPLAY_AUCTIONS)
                .toList();

        if (displayAuctions.isEmpty()) {
            upcomingAuctionsContainer.getChildren().add(loadEmptyCard(
                    "No auctions available yet",
                    "When a seller creates a new auction or one begins, this list updates automatically."
            ));
            return;
        }

        for (AuctionItem auction : displayAuctions) {
            upcomingAuctionsContainer.getChildren().add(loadAuctionCard(auction, bidCounts.getOrDefault(auction.getId(), 0)));
        }
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac render announcement cards.
    private void renderAnnouncementCards(List<HomepageAnnouncement> announcements, Map<Integer, AuctionItem> auctionLookup) {
        announcementsContainer.getChildren().clear();

        List<HomepageAnnouncement> displayAnnouncements = announcements.stream()
                .limit(MAX_DISPLAY_ANNOUNCEMENTS)
                .toList();

        if (displayAnnouncements.isEmpty()) {
            announcementsContainer.getChildren().add(loadEmptyCard(
                    "No admin announcements yet",
                    "Announcements, auction schedules, and featured items will appear here after admins publish updates."
            ));
            return;
        }

        for (HomepageAnnouncement announcement : displayAnnouncements) {
            announcementsContainer.getChildren().add(loadAnnouncementCard(
                    announcement,
                    auctionLookup.get(announcement.getLinkedAuctionId())
            ));
        }
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac load auction card.
    private VBox loadAuctionCard(AuctionItem auction, int bidCount) {
        LoadedView<HomeAuctionCardController> view = FxmlRuntime.loadView(
                HomeViewController.class,
                "home-auction-card.fxml",
                "component"
        );
        view.controller().setAuction(auction, bidCount);
        return (VBox) view.root();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac load home snapshot.
    private HomeSnapshot loadHomeSnapshot() {
        List<AuctionItem> auctions = auctionController == null ? List.of() : auctionController.getAllAuctions();
        List<HomepageAnnouncement> announcements = homepageController == null ? List.of() : homepageController.getAllAnnouncements();
        Map<Integer, Integer> bidCounts = new HashMap<>();

        if (auctionController != null) {
            for (BidTransaction bid : auctionController.getAllBids()) {
                bidCounts.merge(bid.getAuctionId(), 1, Integer::sum);
            }
        }

        return new HomeSnapshot(auctions, announcements, bidCounts);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply home snapshot.
    private void applyHomeSnapshot(HomeSnapshot snapshot) {
        Map<Integer, AuctionItem> auctionLookup = new HashMap<>();
        int runningCount = 0;
        int upcomingCount = 0;

        for (AuctionItem auction : snapshot.auctions()) {
            auctionLookup.put(auction.getId(), auction);
            if (auction.getStatus() == AuctionStatus.RUNNING) {
                runningCount++;
            } else if (auction.getStatus() == AuctionStatus.OPEN) {
                upcomingCount++;
            }
        }

        statRunningValue.setText(String.valueOf(runningCount));
        statUpcomingValue.setText(String.valueOf(upcomingCount));
        statAnnouncementValue.setText(String.valueOf(snapshot.announcements().size()));

        renderAuctionCards(snapshot.auctions(), snapshot.bidCounts());
        renderAnnouncementCards(snapshot.announcements(), auctionLookup);
        requestScrollRevealUpdate();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac load announcement card.
    private VBox loadAnnouncementCard(HomepageAnnouncement announcement, AuctionItem linkedAuction) {
        LoadedView<HomeAnnouncementCardController> view = FxmlRuntime.loadView(
                HomeViewController.class,
                "home-announcement-card.fxml",
                "component"
        );
        view.controller().setAnnouncement(announcement, linkedAuction);
        return (VBox) view.root();
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac load empty card.
    private VBox loadEmptyCard(String titleText, String bodyText) {
        LoadedView<HomeEmptyCardController> view = FxmlRuntime.loadView(
                HomeViewController.class,
                "home-empty-card.fxml",
                "component"
        );
        view.controller().setContent(titleText, bodyText);
        return (VBox) view.root();
    }
    // Phuong thuc: thuc hien chuc nang play intro animation trong lop HomeViewController.
    private void playIntroAnimation() {
        showStaticState();
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create reveal.
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
    // Phuong thuc: khoi dong hoac khoi tao tien trinh start ambient motion.
    private void startAmbientMotion() {
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create float animation.
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
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac create pulse animation.
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
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac stop animations.
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
    // Phuong thuc: thuc hien chuc nang reset intro state trong lop HomeViewController.
    private void resetIntroState() {
        prepareReveal(topBar, 0, -30);
        prepareReveal(heroPanel, -54, 0);
        prepareReveal(featurePanel, 58, 0);
        prepareReveal(statsRow, 0, 28);
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show static state.
    private void showStaticState() {
        showNode(topBar);
        showNode(heroPanel);
        showNode(featurePanel);
        showNode(statsRow);
        showNode(feedSection);
        showNode(auctionColumn);
        showNode(announcementColumn);
    }
    // Phuong thuc: thuc hien chuc nang prepare reveal trong lop HomeViewController.
    private void prepareReveal(Node node, double translateX, double translateY) {
        node.setOpacity(0);
        node.setTranslateX(translateX);
        node.setTranslateY(translateY);
        node.setScaleX(0.96);
        node.setScaleY(0.96);
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac show node.
    private void showNode(Node node) {
        node.setOpacity(1);
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
    }
    // Phuong thuc: thuc hien chuc nang reset orb trong lop HomeViewController.
    private void resetOrb(Node node) {
        node.setTranslateX(0);
        node.setTranslateY(0);
        node.setScaleX(1);
        node.setScaleY(1);
    }
    // Phuong thuc: thuc hien chuc nang configure scroll reveal targets trong lop HomeViewController.
    private void configureScrollRevealTargets() {
        scrollRevealTargets.clear();
        scrollRevealTargets.add(new ScrollRevealTarget(feedSection, 0, 84, 0.92, 0.0, 0.84, 0.38));
        scrollRevealTargets.add(new ScrollRevealTarget(auctionColumn, -46, 30, 0.96, 0.0, 0.82, 0.30));
        scrollRevealTargets.add(new ScrollRevealTarget(announcementColumn, 46, 30, 0.96, 0.0, 0.76, 0.30));
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac register scroll reveal listeners.
    private void registerScrollRevealListeners() {
        landingScrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> updateScrollReveal());
        landingScrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> updateScrollReveal());
        landingScrollPane.contentProperty().addListener((observable, oldValue, newValue) -> requestScrollRevealUpdate());
    }
    // Phuong thuc: thuc hien chuc nang reset scroll reveal state trong lop HomeViewController.
    private void resetScrollRevealState() {
        scrollRevealProgress.clear();
        showNode(feedSection);
        showNode(auctionColumn);
        showNode(announcementColumn);
    }
    // Phuong thuc: thuc hien chuc nang request scroll reveal update trong lop HomeViewController.
    private void requestScrollRevealUpdate() {
        showNode(feedSection);
        showNode(auctionColumn);
        showNode(announcementColumn);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update scroll reveal.
    private void updateScrollReveal() {
        showNode(feedSection);
        showNode(auctionColumn);
        showNode(announcementColumn);
    }
    // Phuong thuc: thuc hien chuc nang compute scroll reveal progress trong lop HomeViewController.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply scroll reveal.
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
    // Phuong thuc: thuc hien chuc nang clamp trong lop HomeViewController.
    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
    // Phuong thuc: thuc hien chuc nang ease trong lop HomeViewController.
    private double ease(double progress) {
        return progress * progress * (3 - (2 * progress));
    }
    // Phuong thuc: thuc hien chuc nang scroll reveal target trong lop HomeViewController.
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
    // Phuong thuc: thuc hien chuc nang home snapshot trong lop HomeViewController.
    private record HomeSnapshot(
            List<AuctionItem> auctions,
            List<HomepageAnnouncement> announcements,
            Map<Integer, Integer> bidCounts
    ) {
    }
}
