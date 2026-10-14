package org.opendatamesh.platform.up.executor.gitlab.exceptions;

import jakarta.ws.rs.core.Response;

public class ServiceUnavailableException extends ExecutorGitlabApiException {

    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorName() {
        return getClass().getSimpleName();
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.SERVICE_UNAVAILABLE;
    }
}
