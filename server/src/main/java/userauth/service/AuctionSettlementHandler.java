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
