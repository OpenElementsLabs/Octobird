package org.hiero.bot.scheduled;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskManager {

    private final ScheduledExecutorService executor;

    public ScheduledTaskManager() {
        this.executor = Executors.newScheduledThreadPool(1, r -> {
            final Thread t = Thread.ofVirtual().unstarted(r);
            t.setName("scheduled-task");
            return t;
        });
    }

    public void scheduleAtFixedRate(final Runnable task, final long initialDelay,
                                    final long period, final TimeUnit unit) {
        executor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public void scheduleWithFixedDelay(final Runnable task, final long initialDelay,
                                       final long delay, final TimeUnit unit) {
        executor.scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
