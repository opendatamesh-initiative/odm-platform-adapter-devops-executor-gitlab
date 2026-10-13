package org.executor.exceptions;

import jakarta.ws.rs.core.Response;

public abstract class ExecutorStarterApiException extends RuntimeException {

    private static final long serialVersionUID = 3876573329263306459L;

    public ExecutorStarterApiException() {
        super();
    }

    public ExecutorStarterApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ExecutorStarterApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutorStarterApiException(String message) {
        super(message);
    }

    public ExecutorStarterApiException(Throwable cause) {
        super(cause);
    }

    public abstract String getErrorName();

    public abstract Response.Status getStatus();
}
