package userauth.client.remote;

/** Lỗi nghiệp vụ hoặc kỹ thuật được server trả về. */
public class RemoteServerException extends RuntimeException {
    private final String errorType;

    public RemoteServerException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public String getErrorType() {
        return errorType;
    }
}
