package userauth.network;

import java.io.Serializable;

public class AuctionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final Object data;
    private final String errorType;
    private final String errorMessage;

    private AuctionResponse(boolean success, Object data, String errorType, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public static AuctionResponse ok(Object data) {
        return new AuctionResponse(true, data, null, null);
    }

    public static AuctionResponse fail(Throwable throwable) {
        String type = throwable == null ? "UNKNOWN" : throwable.getClass().getSimpleName();
        String message = throwable == null ? "Unknown server error." : throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = type;
        }
        return new AuctionResponse(false, null, type, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
