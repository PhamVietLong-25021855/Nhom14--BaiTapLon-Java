package userauth.client.network;

import userauth.common.RemoteAction;
import userauth.common.RemoteRequest;
import userauth.common.RemoteResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class RemoteApiClient {
    private static final String CONNECTION_FAILURE_PREFIX = "Unable to connect to auction server at ";
    private static final String HOST_PROPERTY = "app.server.host";
    private static final String HOST_ENV = "APP_SERVER_HOST";
    private static final String PORT_PROPERTY = "app.server.port";
    private static final String PORT_ENV = "APP_SERVER_PORT";
    private static final int DEFAULT_PORT = 9999;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private final String host;
    private final int port;

    public RemoteApiClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static RemoteApiClient fromConfiguration() {
        String host = resolveText(HOST_PROPERTY, HOST_ENV, "127.0.0.1");
        int port = resolvePort();
        return new RemoteApiClient(host, port);
    }

    public RemoteResponse send(RemoteAction action, Object... arguments) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
                output.flush();
                try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                    output.writeObject(new RemoteRequest(action, arguments));
                    output.flush();

                    Object response = input.readObject();
                    if (response instanceof RemoteResponse remoteResponse) {
                        return remoteResponse;
                    }
                    throw new IllegalStateException("The server returned an invalid response.");
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                    CONNECTION_FAILURE_PREFIX + host + ":" + port + ".",
                    ex
            );
        }
    }

    public boolean ping() {
        try {
            RemoteResponse response = send(RemoteAction.SYSTEM_PING);
            return response.isSuccess() && "PONG".equals(response.payloadAsString());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getEndpoint() {
        return host + ":" + port;
    }

    public static boolean isConnectionFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (isConnectionFailureMessage(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static boolean isConnectionFailureMessage(String message) {
        return message != null && message.startsWith(CONNECTION_FAILURE_PREFIX);
    }

    private static int resolvePort() {
        String propertyValue = System.getProperty(PORT_PROPERTY);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Integer.parseInt(propertyValue.trim());
        }

        String envValue = System.getenv(PORT_ENV);
        if (envValue != null && !envValue.isBlank()) {
            return Integer.parseInt(envValue.trim());
        }

        return DEFAULT_PORT;
    }

    private static String resolveText(String propertyKey, String envKey, String fallback) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return fallback;
    }
}
