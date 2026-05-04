package userauth.gui.fxml;

import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class AuctionViewFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private AuctionViewFormatter() {
    }

    public static String formatMoney(double amount) {
        return String.format("%,.0f", amount);
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

    public static String formatScheduleRange(long startTime, long endTime) {
        return formatDateTime(startTime) + " - " + formatDateTime(endTime);
    }

    public static String formatScheduleRange(AuctionItem item) {
        return formatScheduleRange(item.getStartTime(), item.getEndTime());
    }

    private static String formatDurationText(long milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        return minutes + " " + UiText.text("min") + " " + seconds + " " + UiText.text("sec");
    }
}
