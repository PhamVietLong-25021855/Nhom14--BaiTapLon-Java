package userauth.service;

import userauth.exception.ValidationException;
import userauth.model.AuctionItem;
import userauth.model.AuctionStatus;

interface AuctionSettlementHandler {
    AuctionStatus targetStatus();
    void validate(AuctionItem item) throws ValidationException;
    void apply(AuctionItem item, long now);
    String summary(AuctionItem item);
}

final class AuctionSettlementHandlerFactory {
    AuctionSettlementHandler create(AuctionStatus targetStatus) {
        return switch (targetStatus) {
            case PAID -> new PaidSettlementHandler();
            case CANCELED -> new CanceledSettlementHandler();
            default -> throw new IllegalArgumentException("Unsupported settlement status: " + targetStatus);
        };
    }

    private static final class PaidSettlementHandler implements AuctionSettlementHandler {
        @Override public AuctionStatus targetStatus() { return AuctionStatus.PAID; }
        @Override public void validate(AuctionItem item) throws ValidationException {
            if (item.getWinnerId() <= 0)
                throw new ValidationException("A finished auction without a winner cannot be marked as paid.");
        }
        @Override public void apply(AuctionItem item, long now) {
            item.setStatus(AuctionStatus.PAID);
            item.setUpdatedAt(now);
        }
        @Override public String summary(AuctionItem item) { return "Auction marked as paid."; }
    }

    private static final class CanceledSettlementHandler implements AuctionSettlementHandler {
        @Override public AuctionStatus targetStatus() { return AuctionStatus.CANCELED; }
        @Override public void validate(AuctionItem item) {}
        @Override public void apply(AuctionItem item, long now) {
            item.setStatus(AuctionStatus.CANCELED);
            item.setWinnerId(-1);
            item.setUpdatedAt(now);
        }
        @Override public String summary(AuctionItem item) { return "Auction result has been cancelled."; }
    }
}
