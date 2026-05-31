package userauth.network;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String action;
    private final Map<String, Object> params;
    private final String sessionToken;

    public AuctionRequest(String action) {
        this(action, new LinkedHashMap<>(), null);
    }

    public AuctionRequest(String action, Map<String, Object> params) {
        this(action, params, null);
    }

    public AuctionRequest(String action, Map<String, Object> params, String sessionToken) {
        this.action = action;
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
        this.sessionToken = sessionToken;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public Object get(String key) {
        return params.get(key);
    }

    public String getSessionToken() {
        return sessionToken;
    }
}
