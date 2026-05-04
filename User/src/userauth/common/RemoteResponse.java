package userauth.common;

import java.io.Serial;
import java.io.Serializable;

public final class RemoteResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final Object payload;
    private final String message;
    private final String errorType;

    private RemoteResponse(boolean success, Object payload, String message, String errorType) {
        this.success = success;
        this.payload = payload;
        this.message = message;
        this.errorType = errorType;
    }

    public static RemoteResponse success(Object payload) {
        return new RemoteResponse(true, payload, null, null);
    }

    public static RemoteResponse failure(String message, String errorType) {
        return new RemoteResponse(false, null, message, errorType);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getPayload() {
        return payload;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorType() {
        return errorType;
    }

    public String payloadAsString() {
        return payload instanceof String value ? value : null;
    }

    public <T> T payloadAs(Class<T> type) {
        return payload == null ? null : type.cast(payload);
    }
}
