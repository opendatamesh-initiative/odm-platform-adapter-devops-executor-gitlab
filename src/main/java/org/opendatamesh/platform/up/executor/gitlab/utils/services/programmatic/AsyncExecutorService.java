package org.opendatamesh.platform.up.executor.gitlab.utils.services.programmatic;

/**
 * Runs work in the background on Quarkus-managed virtual threads.
 * Use it for blocking I/O: HTTP calls, database access, file reads.
 * For CPU-heavy work, or code that blocks inside {@code synchronized} or native calls,
 * use {@link PlatformThreadExecutorService}.
 * The caller's MDC (correlation id) is copied into the task.
 * Configured with {@code quarkus.virtual-threads.*}.
 */
public interface AsyncExecutorService {

    void execute(Runnable runnable);
}
