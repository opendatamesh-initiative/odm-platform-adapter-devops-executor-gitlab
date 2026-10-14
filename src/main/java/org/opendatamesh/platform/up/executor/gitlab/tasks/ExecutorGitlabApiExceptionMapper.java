package org.opendatamesh.platform.up.executor.gitlab.tasks;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.ExecutorGitlabApiException;

@Provider
public class ExecutorGitlabApiExceptionMapper implements ExceptionMapper<ExecutorGitlabApiException> {

    @Override
    public Response toResponse(ExecutorGitlabApiException exception) {
        return Response.status(exception.getStatus())
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
