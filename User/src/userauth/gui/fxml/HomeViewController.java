package userauth.gui.fxml;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import userauth.controller.AuctionController;
import userauth.controller.HomepageController;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import userauth.model.BidTransaction;
import userauth.model.HomepageAnnouncement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeViewController {
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
    private final Timeline refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), event -> refreshContent())
    );

    private long refreshTicket;
    private AuctionController auctionController;
    private HomepageController homepageController;

    @FXML
    private void initialize() {
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
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
        refreshContent();
        if (refreshTimeline.getStatus() != Animation.Status.RUNNING) {
            refreshTimeline.play();
        }
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }

    public void deactivate() {
        refreshTicket++;
        refreshTimeline.stop();
        showStaticState();
        resetOrb(orbOne);
        resetOrb(orbTwo);
        resetOrb(orbThree);
    }

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

    private VBox loadAuctionCard(AuctionItem auction, int bidCount) {
        LoadedView<HomeAuctionCardController> view = FxmlRuntime.loadView(
                HomeViewController.class,
                "home-auction-card.fxml",
                "component"
        );
        view.controller().setAuction(auction, bidCount);
        return (VBox) view.root();
    }

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

    private VBox loadAnnouncementCard(HomepageAnnouncement announcement, AuctionItem linkedAuction) {
        LoadedView<HomeAnnouncementCardController> view = FxmlRuntime.loadView(
                HomeViewController.class,
                "home-announcement-card.fxml",
                "component"
        );
        view.controller().setAnnouncement(announcement, linkedAuction);
        return (VBox) view.root();
    }

    private VBox loadEmptyCard(String titleText, String bodyText) {
        LoadedView<HomeEmptyCardController> view = FxmlRuntime.loadView(
                HomeViewController.class,
                "home-empty-card.fxml",
                "component"
        );
        view.controller().setContent(titleText, bodyText);
        return (VBox) view.root();
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

    private void requestScrollRevealUpdate() {
        showNode(feedSection);
        showNode(auctionColumn);
        showNode(announcementColumn);
    }

    private record HomeSnapshot(
            List<AuctionItem> auctions,
            List<HomepageAnnouncement> announcements,
            Map<Integer, Integer> bidCounts
    ) {
    }
}
