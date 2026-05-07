package userauth.client.remote;

import userauth.network.AuctionRequest;
import userauth.network.AuctionResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client socket dùng chung cho toàn bộ RemoteService ở JavaFX Client. */
public final class RemoteAuctionClient {
    private final String host;
    private final int port;
    private final int timeoutMs;

    public RemoteAuctionClient() {
        this(RemoteClientConfig.host(), RemoteClientConfig.port(), 10_000);
    }

    public RemoteAuctionClient(String host, int port, int timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    public Object call(String action, Object... keyValues) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            params.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        AuctionRequest request = new AuctionRequest(action, params);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
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
}
