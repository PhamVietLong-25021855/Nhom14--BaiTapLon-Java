package userauth.server;

import userauth.network.AuctionRequest;
import userauth.network.AuctionResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Socket server đơn giản: mỗi kết nối nhận một AuctionRequest và trả một AuctionResponse.
 * Tầng này không chứa logic nghiệp vụ; logic được chuyển vào Controller/Service thông qua AuctionRequestHandler.
 */
public final class AuctionSocketServer implements AutoCloseable {
    private static final int MAX_CLIENT_THREADS = Math.max(16, Math.min(64, Runtime.getRuntime().availableProcessors() * 4));
    private static final int CLIENT_QUEUE_CAPACITY = MAX_CLIENT_THREADS * 4;

    private final String bindHost;
    private final int port;
    private final AuctionRequestHandler handler;
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
            }
    );
    private volatile boolean running;
    private ServerSocket serverSocket;

    public AuctionSocketServer(String bindHost, int port, AuctionRequestHandler handler) {
        this.bindHost = bindHost == null || bindHost.isBlank() ? "0.0.0.0" : bindHost.trim();
        this.port = port;
        this.handler = handler;
    }

    public AuctionSocketServer(int port, AuctionRequestHandler handler) {
        this("0.0.0.0", port, handler);
    }

    public void start() throws Exception {
        running = true;
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(bindHost, port));
        System.out.println("[AuctionServer] Listening on " + bindHost + ":" + port);
        System.out.println("[AuctionServer] Clients must connect to this machine's public IP/domain and TCP port " + port + ".");
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
        try (Socket client = socket;
             ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(client.getInputStream())) {
            Object object = input.readObject();
            AuctionResponse response;
            if (object instanceof AuctionRequest request) {
                response = handler.handle(request);
            } else {
                response = AuctionResponse.fail(new IllegalArgumentException("Invalid request object."));
            }
            output.writeObject(response);
            output.flush();
        } catch (Exception ex) {
            System.err.println("[AuctionServer] Client handling error: " + ex.getMessage());
        }
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
