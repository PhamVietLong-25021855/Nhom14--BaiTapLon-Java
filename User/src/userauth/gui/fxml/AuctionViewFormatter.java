package userauth.gui.fxml;

import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

public final class AuctionViewFormatter {
    private AuctionViewFormatter() {
    }

    public static String formatMoney(double amount) {
        return String.format("%,.0f", amount);
    }

    public static String formatDuration(AuctionItem item) {
        long minutes = Math.max(1, (item.getEndTime() - item.getStartTime()) / 60000);
        return minutes + " phut";
    }

    public static String formatRemaining(long endTime) {
        long remainingMs = endTime - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return "Het gio";
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
            return "Chua bat dau (" + formatDurationText(item.getStartTime() - now) + ")";
        }

        long remainingMs = item.getEndTime() - now;
        if (remainingMs <= 0) {
            return "Het gio";
        }
        return formatDurationText(remainingMs);
    }

    private static String formatDurationText(long milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        return minutes + " phut " + seconds + "s";
    }
}
