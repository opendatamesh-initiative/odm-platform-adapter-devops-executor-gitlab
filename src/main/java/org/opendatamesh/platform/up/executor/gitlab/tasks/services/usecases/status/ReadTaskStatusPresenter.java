package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.RunStatus;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCasePresenter;

public interface ReadTaskStatusPresenter extends UseCasePresenter {

    void presentStatus(String pipelineId, RunStatus status);
}
