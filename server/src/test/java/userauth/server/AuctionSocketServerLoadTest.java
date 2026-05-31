package userauth.server;

import org.junit.jupiter.api.Test;
import userauth.network.AuctionRequest;
import userauth.network.AuctionResponse;
import userauth.network.NetworkActions;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSocketServerLoadTest {
    private static final int CONCURRENT_CLIENTS = Math.min(32, Math.max(8, AuctionSocketServer.clientCapacityForTests() / 2));
    private static final int REQUEST_COUNT = CONCURRENT_CLIENTS * 20;

    @Test
    void handlesConcurrentPingClientsWithoutDatabase() throws Exception {
        AuctionSocketServer server = new AuctionSocketServer("127.0.0.1", 0, new AuctionRequestHandler(null));
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, "auction-load-test-server");
        serverThread.setDaemon(true);
        serverThread.start();

        ExecutorService clients = Executors.newFixedThreadPool(CONCURRENT_CLIENTS);
        try {
            int port = awaitLocalPort(server);
            long startedAt = System.nanoTime();
            List<Callable<String>> calls = new ArrayList<>();
            for (int index = 0; index < REQUEST_COUNT; index++) {
                calls.add(() -> ping(port));
            }

            List<Future<String>> results = clients.invokeAll(calls, 30, TimeUnit.SECONDS);
            for (Future<String> result : results) {
                assertTrue(result.isDone() && !result.isCancelled(), "A ping request timed out.");
                assertEquals("PONG", result.get());
            }

            long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            System.out.printf("[SocketLoadTest] %,d PING requests with %d concurrent clients completed in %,d ms.%n",
                    REQUEST_COUNT, CONCURRENT_CLIENTS, elapsedMs);
        } finally {
            clients.shutdownNow();
            server.close();
            serverThread.join(5_000);
        }
    }

    private int awaitLocalPort(AuctionSocketServer server) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            int port = server.localPort();
            if (port > 0) {
                return port;
            }
            Thread.sleep(10);
        }
        throw new IllegalStateException("The load-test server did not start in time.");
    }

    private String ping(int port) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5_000);
            socket.setSoTimeout(10_000);
            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                output.writeObject(new AuctionRequest(NetworkActions.PING));
                output.flush();
                Object object = input.readObject();
                AuctionResponse response = (AuctionResponse) object;
                return (String) response.getData();
            }
        }
    }
}
