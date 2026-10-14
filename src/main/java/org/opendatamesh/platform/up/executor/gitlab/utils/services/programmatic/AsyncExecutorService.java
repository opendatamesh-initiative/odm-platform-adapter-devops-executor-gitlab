package org.opendatamesh.platform.up.executor.gitlab.utils.services.programmatic;

public interface AsyncExecutorService {

    void execute(Runnable runnable);
}
