package org.executor.exceptions;

import jakarta.ws.rs.core.Response;

public class InternalException extends ExecutorStarterApiException {

    private static final long serialVersionUID = 1L;

    public InternalException(String message) {
        super(message);
    }

    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorName() {
        return getClass().getSimpleName();
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }
}
