package userauth.remote;

import userauth.network.AuctionRequest;
import userauth.network.AuctionResponse;

import java.io.ObjectInputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;

/** Client socket dùng chung cho toàn bộ RemoteService ở JavaFX Client. */
public final class RemoteAuctionClient {
    private final String host;
    private final int port;
    private final int timeoutMs;
    private final boolean tlsEnabled;
    private volatile String sessionToken;

    public RemoteAuctionClient() {
        this(RemoteClientConfig.host(), RemoteClientConfig.port(), 10_000, RemoteClientConfig.tlsEnabled());
    }

    public RemoteAuctionClient(String host, int port, int timeoutMs) {
        this(host, port, timeoutMs, false);
    }

    public RemoteAuctionClient(String host, int port, int timeoutMs, boolean tlsEnabled) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
        this.tlsEnabled = tlsEnabled;
    }

    public Object call(String action, Object... keyValues) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            params.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        AuctionRequest request = new AuctionRequest(action, params, sessionToken);
        try (Socket socket = createSocket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                input.setObjectInputFilter(RemoteAuctionClient::filterResponse);
                output.writeObject(request);
                output.flush();
                Object responseObject = input.readObject();
                if (!(responseObject instanceof AuctionResponse response)) {
                    throw new IllegalStateException("Server returned an invalid response.");
                }
                if (!response.isSuccess()) {
                    throw new RemoteServerException(response.getErrorType(), response.getErrorMessage());
                }
                return response.getData();
            }
        } catch (RemoteServerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot connect to auction server " + host + ":" + port + ". " + ex.getMessage(), ex);
        }
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public void clearSession() {
        sessionToken = null;
    }

    public boolean hasSession() {
        return sessionToken != null && !sessionToken.isBlank();
    }

    private Socket createSocket() throws Exception {
        return tlsEnabled
                ? SSLSocketFactory.getDefault().createSocket()
                : new Socket();
    }

    static ObjectInputFilter.Status filterResponse(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 32 || info.references() > 100_000 || info.streamBytes() > 32L * 1024 * 1024) {
            return ObjectInputFilter.Status.REJECTED;
        }
        if (info.arrayLength() >= 0 && info.arrayLength() > 16L * 1024 * 1024) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        String packageName = type.getPackageName();
        if ("userauth.network".equals(packageName)
                || "userauth.model".equals(packageName)
                || "java.util".equals(packageName)
                || type == Object.class
                || type == String.class
                || type == Number.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Short.class
                || type == Byte.class
                || type == Boolean.class
                || type == Character.class
                || type == Enum.class) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }
}
