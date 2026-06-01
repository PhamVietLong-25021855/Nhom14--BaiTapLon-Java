package userauth.gui.fxml.shared;

import userauth.common.AuctionRules;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class AuctionViewFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIME_SECONDS_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    private AuctionViewFormatter() {
    }

    public static String formatMoney(double amount) {
        return String.format(Locale.US, "%,.0f", amount).replace(',', '.');
    }

    public static String formatPercent(double ratio) {
        return PERCENT_FORMAT.format(ratio * 100.0d) + "%";
    }

    public static String formatBidStep(AuctionItem item) {
        if (item == null) {
            return "-";
        }
        return formatMoney(item.getBidStep()) + " (" + formatPercent(safeBidStepRatio(item)) + ")";
    }

    public static String formatMinimumBid(AuctionItem item) {
        if (item == null) {
            return "-";
        }
        return formatMoney(item.getCurrentHighestBid() + item.getBidStep());
    }

    public static String formatDuration(AuctionItem item) {
        long minutes = Math.max(1, (item.getEndTime() - item.getStartTime()) / 60000);
        return minutes + " " + UiText.text("min");
    }

    public static String formatRemaining(long endTime) {
        long remainingMs = endTime - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return UiText.text("Ended");
        }
        return formatDurationText(remainingMs);
    }

    public static String formatTimeLeft(AuctionItem item) {
        long now = System.currentTimeMillis();
        AuctionStatus status = item.getStatus();

        if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED || status == AuctionStatus.PAID) {
            return "-";
        }
        if (now < item.getStartTime()) {
            return UiText.text("Not started") + " (" + formatDurationText(item.getStartTime() - now) + ")";
        }

        long remainingMs = item.getEndTime() - now;
        if (remainingMs <= 0) {
            return UiText.text("Ended");
        }
        return formatDurationText(remainingMs);
    }

    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    public static String formatDateTimeWithSeconds(long timestamp) {
        return DATE_TIME_SECONDS_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    public static String formatTimeWithSeconds(long timestamp) {
        return TIME_SECONDS_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }

    public static String formatScheduleRange(long startTime, long endTime) {
        return formatDateTime(startTime) + " - " + formatDateTime(endTime);
    }

    public static String formatScheduleRange(AuctionItem item) {
        return formatScheduleRange(item.getStartTime(), item.getEndTime());
    }

    public static String formatAntiSnipingSummary(AuctionItem item) {
        if (item == null) {
            return "0/" + AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS;
        }
        return item.getAntiSnipingExtensionCount()
                + "/" + AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS
                + " | closes at "
                + formatDateTimeWithSeconds(item.getEndTime());
    }

    private static double safeBidStepRatio(AuctionItem item) {
        if (item == null || item.getStartPrice() <= 0) {
            return AuctionRules.MIN_BID_STEP_PERCENT;
        }
        return item.getBidStep() / item.getStartPrice();
    }

    private static String formatDurationText(long milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        return minutes + " " + UiText.text("min") + " " + seconds + " " + UiText.text("sec");
    }
}
