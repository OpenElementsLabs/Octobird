package com.openelements.octobird.scheduled;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages recurring background tasks using a single-threaded
 * {@link java.util.concurrent.ScheduledExecutorService} backed by virtual threads (Project Loom).
 *
 * <p>Tasks are run on lightweight virtual threads so that blocking operations (e.g. GitHub API
 * calls) do not stall the scheduler thread.
 */
public class ScheduledTaskManager {

    private final ScheduledExecutorService executor;

    /**
     * Creates a new {@code ScheduledTaskManager} backed by a one-thread virtual-thread pool.
     */
    public ScheduledTaskManager() {
        this.executor = Executors.newScheduledThreadPool(1, r -> {
            final Thread t = Thread.ofVirtual().unstarted(r);
            t.setName("scheduled-task");
            return t;
        });
    }

    /**
     * Schedules a task to run at a fixed rate.
     *
     * @param task         the task to execute
     * @param initialDelay delay before the first execution
     * @param period       period between successive executions
     * @param unit         time unit for {@code initialDelay} and {@code period}
     * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate
     */
    public void scheduleAtFixedRate(final Runnable task, final long initialDelay,
                                    final long period, final TimeUnit unit) {
        executor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Schedules a task to run with a fixed delay between the end of one execution and the start
     * of the next.
     *
     * @param task         the task to execute
     * @param initialDelay delay before the first execution
     * @param delay        delay between the end of one execution and the start of the next
     * @param unit         time unit for {@code initialDelay} and {@code delay}
     * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay
     */
    public void scheduleWithFixedDelay(final Runnable task, final long initialDelay,
                                       final long delay, final TimeUnit unit) {
        executor.scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    /**
     * Initiates an orderly shutdown, waiting up to 10 seconds for running tasks to finish.
     * Calls {@code shutdownNow()} if the timeout elapses or the thread is interrupted.
     */
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
