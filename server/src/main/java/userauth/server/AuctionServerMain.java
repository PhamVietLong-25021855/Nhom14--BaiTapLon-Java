package userauth.server;

/** Entry point chạy riêng phía Server trên VPS. */
public final class AuctionServerMain {
    private static final String PORT_PROPERTY = "app.server.port";
    private static final String PORT_ENV = "APP_SERVER_PORT";
    private static final String BIND_HOST_PROPERTY = "app.server.bind.host";
    private static final String BIND_HOST_ENV = "APP_SERVER_BIND_HOST";
    private static final String DEFAULT_BIND_HOST = "0.0.0.0";

    private AuctionServerMain() {}

    public static void main(String[] args) throws Exception {
        int port = resolvePort();
        String bindHost = resolveBindHost();
        ServerContext context = new ServerContext(true);
        AuctionSocketServer server = new AuctionSocketServer(bindHost, port, new AuctionRequestHandler(context));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
                context.stop();
            } catch (Exception ignored) {
            }
        }, "auction-server-shutdown"));
        server.start();
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
        return 5050;
    }

    private static String resolveBindHost() {
        String propertyValue = System.getProperty(BIND_HOST_PROPERTY);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        String envValue = System.getenv(BIND_HOST_ENV);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return DEFAULT_BIND_HOST;
    }
}
