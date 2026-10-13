package org.executor.utils.usecases;

import org.executor.exceptions.InternalException;
import org.executor.exceptions.ExecutorStarterApiException;

public interface UseCasePresenter {

    default void presentException(ExecutorStarterApiException exception) {
        throw exception;
    }

    default void presentException(Exception exception) {
        throw new InternalException(exception.getMessage(), exception);
    }
}
