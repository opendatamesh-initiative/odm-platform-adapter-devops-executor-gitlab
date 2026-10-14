package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCasePresenter;

public interface StartTaskPresenter extends UseCasePresenter {

    void presentStarted(String pipelineId);
}
