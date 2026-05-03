package userauth.gui.fxml;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class UiAsync {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            new DaemonThreadFactory()
    );

    private UiAsync() {
    }

    static <T> void run(Supplier<T> supplier, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        CompletableFuture.supplyAsync(supplier, EXECUTOR)
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    if (throwable != null) {
                        onError.accept(unwrap(throwable));
                        return;
                    }
                    onSuccess.accept(result);
                }));
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }
        return current;
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ui-async-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
