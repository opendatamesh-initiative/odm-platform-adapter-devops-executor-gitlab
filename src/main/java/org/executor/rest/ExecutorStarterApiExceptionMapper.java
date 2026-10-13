package org.executor.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.executor.exceptions.ExecutorStarterApiException;

@Provider
public class ExecutorStarterApiExceptionMapper implements ExceptionMapper<ExecutorStarterApiException> {

    @Override
    public Response toResponse(ExecutorStarterApiException exception) {
        return Response.status(exception.getStatus())
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
