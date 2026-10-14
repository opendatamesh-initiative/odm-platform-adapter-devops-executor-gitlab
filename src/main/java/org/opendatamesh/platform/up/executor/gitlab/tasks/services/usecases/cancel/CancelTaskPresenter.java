package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCasePresenter;

public interface CancelTaskPresenter extends UseCasePresenter {

    void presentCancelRequested(String pipelineId);
}
