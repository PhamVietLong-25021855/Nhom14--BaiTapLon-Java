package userauth.exception;

// File note: Ngoại lệ báo mức bid hiện tại không hợp lệ theo luật đấu giá.
public class InvalidBidException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidBidException(String message) {
        super(message);
    }
}

