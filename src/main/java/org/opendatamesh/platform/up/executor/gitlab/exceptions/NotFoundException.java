package org.opendatamesh.platform.up.executor.gitlab.exceptions;

import jakarta.ws.rs.core.Response;

public class NotFoundException extends ExecutorGitlabApiException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorName() {
        return getClass().getSimpleName();
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.NOT_FOUND;
    }
}
