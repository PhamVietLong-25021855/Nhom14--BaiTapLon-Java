package userauth.network;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gói yêu cầu gửi từ Client sang Server qua Socket.
 * Mỗi request gồm tên hành động và danh sách tham số dạng key-value.
 */
public class AuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String action;
    private final Map<String, Object> params;

    public AuctionRequest(String action) {
        this(action, new LinkedHashMap<>());
    }

    public AuctionRequest(String action, Map<String, Object> params) {
        this.action = action;
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
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
}
