package org.opendatamesh.platform.up.executor.gitlab.utils.usecases;

import org.opendatamesh.platform.up.executor.gitlab.exceptions.InternalException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.ExecutorGitlabApiException;

public interface UseCasePresenter {

    default void presentException(ExecutorGitlabApiException exception) {
        throw exception;
    }

    default void presentException(Exception exception) {
        throw new InternalException(exception.getMessage(), exception);
    }
}
