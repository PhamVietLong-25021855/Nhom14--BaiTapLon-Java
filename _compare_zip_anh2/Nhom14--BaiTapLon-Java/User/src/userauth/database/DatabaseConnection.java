package userauth.database;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class DatabaseConnection {
    private static final DatabaseConfig CONFIG = DatabaseConfig.load();
    private static final int MAX_POOLED_CONNECTIONS = Math.max(2, Math.min(6, Runtime.getRuntime().availableProcessors()));
    private static final long POOL_WAIT_TIMEOUT_MS = 5_000;
    private static final BlockingQueue<Connection> IDLE_CONNECTIONS = new ArrayBlockingQueue<>(MAX_POOLED_CONNECTIONS);
    private static final AtomicInteger CREATED_CONNECTIONS = new AtomicInteger();

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("PostgreSQL JDBC driver not found. Make sure the PostgreSQL driver is available.", ex);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::closeIdleConnections, "db-pool-shutdown"));
    }

    private DatabaseConnection() {
    }

    public static DatabaseConfig getConfig() {
        return CONFIG;
    }

    public static Connection openServerConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getServerJdbcUrl(),
                CONFIG.getUsername(),
                CONFIG.getPassword()
        );
    }

    public static Connection openDatabaseConnection() throws SQLException {
        while (true) {
            Connection idleConnection = IDLE_CONNECTIONS.poll();
            if (idleConnection != null) {
                if (isReusable(idleConnection)) {
                    return wrapPooledConnection(idleConnection);
                }
                retire(idleConnection);
                continue;
            }

            int created = CREATED_CONNECTIONS.get();
            if (created < MAX_POOLED_CONNECTIONS && CREATED_CONNECTIONS.compareAndSet(created, created + 1)) {
                try {
                    return wrapPooledConnection(createPhysicalDatabaseConnection());
                } catch (SQLException ex) {
                    CREATED_CONNECTIONS.decrementAndGet();
                    throw ex;
                }
            }

            try {
                Connection waitedConnection = IDLE_CONNECTIONS.poll(POOL_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (waitedConnection == null) {
                    throw new SQLException("The PostgreSQL connection pool is exhausted. Please try again.");
                }
                if (isReusable(waitedConnection)) {
                    return wrapPooledConnection(waitedConnection);
                }
                retire(waitedConnection);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for a PostgreSQL connection.", ex);
            }
        }
    }

    private static Connection createPhysicalDatabaseConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getDatabaseJdbcUrl(),
                CONFIG.getUsername(),
                CONFIG.getPassword()
        );
    }

    private static boolean isReusable(Connection connection) {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException ex) {
            return false;
        }
    }

    private static Connection wrapPooledConnection(Connection connection) {
        InvocationHandler handler = new PooledConnectionHandler(connection);
        return (Connection) Proxy.newProxyInstance(
                DatabaseConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private static void recycle(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            if (connection.isClosed()) {
                retire(connection);
                return;
            }
            if (!connection.getAutoCommit()) {
                connection.setAutoCommit(true);
            }
            if (connection.isReadOnly()) {
                connection.setReadOnly(false);
            }
            connection.clearWarnings();
            if (!IDLE_CONNECTIONS.offer(connection)) {
                retire(connection);
            }
        } catch (SQLException ex) {
            retire(connection);
        }
    }

    private static void retire(Connection connection) {
        if (connection == null) {
            return;
        }
        CREATED_CONNECTIONS.updateAndGet(current -> Math.max(0, current - 1));
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    private static void closeIdleConnections() {
        Connection connection;
        while ((connection = IDLE_CONNECTIONS.poll()) != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private static final class PooledConnectionHandler implements InvocationHandler {
        private Connection delegate;
        private boolean released;

        private PooledConnectionHandler(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if ("close".equals(methodName)) {
                if (!released) {
                    released = true;
                    Connection connection = delegate;
                    delegate = null;
                    recycle(connection);
                }
                return null;
            }

            if ("isClosed".equals(methodName)) {
                return released || delegate == null || delegate.isClosed();
            }

            if ("unwrap".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> type) {
                if (type.isInstance(proxy)) {
                    return proxy;
                }
                ensureOpen();
                return delegate.unwrap(type);
            }

            if ("isWrapperFor".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> type) {
                return type.isInstance(proxy) || (!released && delegate != null && delegate.isWrapperFor(type));
            }

            if ("toString".equals(methodName)) {
                return released ? "PooledConnection[closed]" : "PooledConnection[" + delegate + "]";
            }

            ensureOpen();
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }

        private void ensureOpen() throws SQLException {
            if (released || delegate == null) {
                throw new SQLException("PostgreSQL connection is closed.");
            }
        }
    }
}
