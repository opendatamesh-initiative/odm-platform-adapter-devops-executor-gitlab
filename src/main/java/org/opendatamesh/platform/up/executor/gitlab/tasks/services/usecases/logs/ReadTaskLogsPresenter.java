package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCasePresenter;

import java.time.Instant;

public interface ReadTaskLogsPresenter extends UseCasePresenter {

    void presentLogs(String content, Instant generatedAt);
}
