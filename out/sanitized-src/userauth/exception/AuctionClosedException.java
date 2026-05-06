package userauth.exception;

// File note: Ngoại lệ báo auction không còn hợp lệ để tiếp tục bid hoặc thao tác.
public class AuctionClosedException extends Exception {
    private static final long serialVersionUID = 1L;

    public AuctionClosedException(String message) {
        super(message);
    }
}

