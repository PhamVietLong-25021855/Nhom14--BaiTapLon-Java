package userauth.exception;

public class AuctionClosedException extends Exception {
    private static final long serialVersionUID = 1L;

    public AuctionClosedException(String message) {
        super(message);
    }
}
