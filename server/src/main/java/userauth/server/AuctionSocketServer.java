package userauth.server;

import userauth.network.AuctionRequest;
import userauth.network.AuctionResponse;
import userauth.model.PaymentMethod;
import userauth.model.Role;

import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Socket server đơn giản: mỗi kết nối nhận một AuctionRequest và trả một AuctionResponse.
 * Tầng này không chứa logic nghiệp vụ; logic được chuyển vào Controller/Service thông qua AuctionRequestHandler.
 */
public final class AuctionSocketServer implements AutoCloseable {
    private static final int MAX_CLIENT_THREADS = Math.max(16, Math.min(64, Runtime.getRuntime().availableProcessors() * 4));
    private static final int CLIENT_QUEUE_CAPACITY = MAX_CLIENT_THREADS * 4;
    private static final int CLIENT_ACCEPT_BACKLOG = MAX_CLIENT_THREADS + CLIENT_QUEUE_CAPACITY;
    private static final int CLIENT_READ_TIMEOUT_MS = 10_000;
    private static final long MAX_REQUEST_BYTES = 8L * 1024 * 1024;
    private static final long MAX_ARRAY_LENGTH = 6L * 1024 * 1024;

    private final String bindHost;
    private final int port;
    private final AuctionRequestHandler handler;
    private final boolean tlsEnabled;
    private final ExecutorService clients = new ThreadPoolExecutor(
            MAX_CLIENT_THREADS,
            MAX_CLIENT_THREADS,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(CLIENT_QUEUE_CAPACITY),
            runnable -> {
                Thread thread = new Thread(runnable, "auction-client");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    private volatile boolean running;
    private ServerSocket serverSocket;

    public AuctionSocketServer(String bindHost, int port, AuctionRequestHandler handler) {
        this(bindHost, port, handler, false);
    }

    public AuctionSocketServer(String bindHost, int port, AuctionRequestHandler handler, boolean tlsEnabled) {
        this.bindHost = bindHost == null || bindHost.isBlank() ? "0.0.0.0" : bindHost.trim();
        this.port = port;
        this.handler = handler;
        this.tlsEnabled = tlsEnabled;
    }

    public AuctionSocketServer(int port, AuctionRequestHandler handler) {
        this("0.0.0.0", port, handler, false);
    }

    int localPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? -1 : socket.getLocalPort();
    }

    static int clientCapacityForTests() {
        return MAX_CLIENT_THREADS + CLIENT_QUEUE_CAPACITY;
    }

    public void start() throws Exception {
        running = true;
        serverSocket = tlsEnabled
                ? SSLServerSocketFactory.getDefault().createServerSocket()
                : new ServerSocket();
        serverSocket.bind(new InetSocketAddress(bindHost, port), CLIENT_ACCEPT_BACKLOG);
        int localPort = serverSocket.getLocalPort();
        System.out.println("[AuctionServer] Listening with " + (tlsEnabled ? "TLS" : "plain TCP") + " on " + bindHost + ":" + localPort);
        if (!tlsEnabled) {
            System.err.println("[AuctionServer] WARNING: TLS is disabled. Enable it before exposing the server to untrusted networks.");
        }
        System.out.println("[AuctionServer] Clients must connect to this machine's public IP/domain and TCP port " + localPort + ".");
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                try {
                    clients.submit(() -> handleClient(socket));
                } catch (RejectedExecutionException ex) {
                    closeQuietly(socket);
                    System.err.println("[AuctionServer] Server is busy; rejected one client connection.");
                }
            } catch (SocketException ex) {
                if (running) {
                    throw ex;
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (Socket client = socket) {
            client.setSoTimeout(CLIENT_READ_TIMEOUT_MS);
            try (ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(client.getInputStream())) {
                input.setObjectInputFilter(AuctionSocketServer::filterRequest);
                Object object = input.readObject();
                AuctionResponse response;
                if (object instanceof AuctionRequest request) {
                    response = handler.handle(request);
                } else {
                    response = AuctionResponse.fail(new IllegalArgumentException("Invalid request object."));
                }
                output.writeObject(response);
                output.flush();
            }
        } catch (Exception ex) {
            System.err.println("[AuctionServer] Client handling error: " + ex.getMessage());
        }
    }

    static ObjectInputFilter.Status filterRequest(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 12 || info.references() > 256 || info.streamBytes() > MAX_REQUEST_BYTES) {
            return ObjectInputFilter.Status.REJECTED;
        }
        if (info.arrayLength() >= 0 && info.arrayLength() > MAX_ARRAY_LENGTH) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive()
                || type == AuctionRequest.class
                || type == LinkedHashMap.class
                || type == HashMap.class
                || type == Map.Entry.class
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
                || type == Enum.class
                || type == Role.class
                || type == PaymentMethod.class) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() throws Exception {
        running = false;
        clients.shutdownNow();
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
