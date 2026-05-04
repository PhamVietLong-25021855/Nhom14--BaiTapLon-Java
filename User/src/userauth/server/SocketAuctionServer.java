package userauth.server;

import userauth.common.RemoteRequest;
import userauth.common.RemoteResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SocketAuctionServer {
    private final int port;
    private final ServerRequestDispatcher dispatcher;
    private final ExecutorService clientExecutor;

    public SocketAuctionServer(int port, ServerRequestDispatcher dispatcher) {
        this.port = port;
        this.dispatcher = dispatcher;
        this.clientExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "auction-server-client");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Auction server is listening on port " + port + ".");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientExecutor.submit(() -> handleClient(clientSocket));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to start the auction socket server.", ex);
        }
    }

    private void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            output.flush();
            try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                Object incoming = input.readObject();
                RemoteResponse response;
                if (incoming instanceof RemoteRequest request) {
                    response = dispatcher.handle(request);
                } else {
                    response = RemoteResponse.failure("Invalid request payload.", IllegalArgumentException.class.getSimpleName());
                }
                output.writeObject(response);
                output.flush();
            }
        } catch (Exception ex) {
            System.err.println("SocketAuctionServer client error: " + ex.getMessage());
        }
    }
}
