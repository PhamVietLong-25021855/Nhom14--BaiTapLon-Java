package userauth.model;

public enum AuctionStatus {
    OPEN,       // Created but not started
    RUNNING,    // Active and accepting bids
    FINISHED,   // Time ended
    PAID,       // Paid by winner
    CANCELED;   // Cancelled
}
