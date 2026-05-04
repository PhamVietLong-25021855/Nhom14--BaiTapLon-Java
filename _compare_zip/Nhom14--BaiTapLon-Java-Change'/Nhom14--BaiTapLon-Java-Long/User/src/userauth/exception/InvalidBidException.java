package userauth.exception;

public class InvalidBidException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidBidException(String message) {
        super(message);
    }
}
