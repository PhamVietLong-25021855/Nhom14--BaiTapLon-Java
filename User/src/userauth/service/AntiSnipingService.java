package userauth.service;

import userauth.model.AuctionItem;

public class AntiSnipingService {
    private static final long MILLIS_PER_SECOND = 1_000L;

    // Kiểm tra xem bid hợp lệ tại bidTime có nằm trong vùng cần gia hạn hay không.
    public boolean shouldExtend(AuctionItem auction, long bidTime) {
        if (auction == null || !auction.isAntiSnipingEnabled()) {
            return false;
        }
        if (auction.getExtensionThresholdSeconds() <= 0 || auction.getExtensionDurationSeconds() <= 0) {
            return false;
        }
        if (auction.getMaxExtensionCount() >= 0
                && auction.getExtensionCount() >= auction.getMaxExtensionCount()) {
            return false;
        }

        long remainingTime = auction.getEndTime() - bidTime;
        long thresholdMillis = toMillis(auction.getExtensionThresholdSeconds());

        return remainingTime >= 0 && remainingTime <= thresholdMillis;
    }

    // Nếu đủ điều kiện thì cập nhật endTime mới ngay trên object auction.
    public boolean extendAuction(AuctionItem auction, long bidTime) {
        if (!shouldExtend(auction, bidTime)) {
            return false;
        }

        long extensionMillis = toMillis(auction.getExtensionDurationSeconds());
        auction.setEndTime(auction.getEndTime() + extensionMillis);
        auction.setExtensionCount(auction.getExtensionCount() + 1);
        auction.setUpdatedAt(Math.max(auction.getUpdatedAt(), bidTime));
        return true;
    }

    public long calculateRemainingTime(AuctionItem auction, long bidTime) {
        return auction.getEndTime() - bidTime;
    }

    private long toMillis(int seconds) {
        return seconds * MILLIS_PER_SECOND;
    }
}
