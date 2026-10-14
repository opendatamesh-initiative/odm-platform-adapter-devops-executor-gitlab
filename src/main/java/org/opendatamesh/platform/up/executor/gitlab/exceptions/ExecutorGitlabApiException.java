package org.opendatamesh.platform.up.executor.gitlab.exceptions;

import jakarta.ws.rs.core.Response;

public abstract class ExecutorGitlabApiException extends RuntimeException {

    private static final long serialVersionUID = 3876573329263306459L;

    public ExecutorGitlabApiException() {
        super();
    }

    public ExecutorGitlabApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ExecutorGitlabApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutorGitlabApiException(String message) {
        super(message);
    }

    public ExecutorGitlabApiException(Throwable cause) {
        super(cause);
    }

    public abstract String getErrorName();

    public abstract Response.Status getStatus();
}
