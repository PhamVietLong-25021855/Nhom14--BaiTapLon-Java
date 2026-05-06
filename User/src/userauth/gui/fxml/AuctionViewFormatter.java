package userauth.gui.fxml;

import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop AuctionViewFormatter; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
public final class AuctionViewFormatter {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho formatter.
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    // Ham tao: khoi tao doi tuong AuctionViewFormatter voi cac phu thuoc can thiet.
    private AuctionViewFormatter() {
    }
    // Phuong thuc: bien doi du lieu cho thao tac format money.
    public static String formatMoney(double amount) {
        return String.format("%,.0f", amount);
    }
    // Phuong thuc: bien doi du lieu cho thao tac format duration.
    public static String formatDuration(AuctionItem item) {
        long minutes = Math.max(1, (item.getEndTime() - item.getStartTime()) / 60000);
        return minutes + " " + UiText.text("min");
    }
    // Phuong thuc: bien doi du lieu cho thao tac format remaining.
    public static String formatRemaining(long endTime) {
        long remainingMs = endTime - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return UiText.text("Ended");
        }
        return formatDurationText(remainingMs);
    }
    // Phuong thuc: bien doi du lieu cho thao tac format time left.
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
    // Phuong thuc: bien doi du lieu cho thao tac format date time.
    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
    // Phuong thuc: bien doi du lieu cho thao tac format schedule range.
    public static String formatScheduleRange(long startTime, long endTime) {
        return formatDateTime(startTime) + " - " + formatDateTime(endTime);
    }
    // Phuong thuc: bien doi du lieu cho thao tac format schedule range.
    public static String formatScheduleRange(AuctionItem item) {
        return formatScheduleRange(item.getStartTime(), item.getEndTime());
    }
    // Phuong thuc: bien doi du lieu cho thao tac format duration text.
    private static String formatDurationText(long milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        return minutes + " " + UiText.text("min") + " " + seconds + " " + UiText.text("sec");
    }
}
