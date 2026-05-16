package userauth.client.remote;

/** Đọc cấu hình địa chỉ server cho client remote. */
public final class RemoteClientConfig {
    private static final String DEFAULT_SERVER_HOST = "172.104.50.54";
    private static final int DEFAULT_SERVER_PORT = 5050;

    private RemoteClientConfig() {}

    public static String host() {
        String value = System.getProperty("app.server.host");
        if (value == null || value.isBlank()) {
            value = System.getenv("APP_SERVER_HOST");
        }
        return value == null || value.isBlank() ? DEFAULT_SERVER_HOST : value.trim();
    }

    public static int port() {
        String value = System.getProperty("app.server.port");
        if (value == null || value.isBlank()) {
            value = System.getenv("APP_SERVER_PORT");
        }
        return value == null || value.isBlank() ? DEFAULT_SERVER_PORT : Integer.parseInt(value.trim());
    }
}
