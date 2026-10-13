package org.executor.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.executor.exceptions.BadRequestException;
import org.executor.resources.TaskCancelCommandRes;
import org.executor.resources.TaskLogsRes;
import org.executor.resources.TaskStartCommandRes;
import org.executor.resources.TaskStartResultRes;
import org.executor.resources.TaskStatusRes;
import org.executor.services.SimulatedRunStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Path("/api/v2/up/executor/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ExecutorTaskResource {

    private static final Logger log = LoggerFactory.getLogger(ExecutorTaskResource.class);

    private static final String SECRET_HEADER_PREFIX = "x-odm-";

    private final SimulatedRunStore simulatedRunStore;

    @Inject
    public ExecutorTaskResource(SimulatedRunStore simulatedRunStore) {
        this.simulatedRunStore = simulatedRunStore;
    }

    @POST
    @Path("/start")
    public TaskStartResultRes start(TaskStartCommandRes request, @Context HttpHeaders headers) {
        if (request == null || request.getExecutorParameters() == null) {
            throw new BadRequestException("executorParameters is required");
        }
        String providerRunId = simulatedRunStore.start(request, secretHeaderNames(headers));
        log.info("Started run {}", providerRunId);
        TaskStartResultRes result = new TaskStartResultRes();
        result.setProviderRunId(providerRunId);
        return result;
    }

    @POST
    @Path("/cancel")
    public Response cancel(TaskCancelCommandRes request) {
        log.info("Cancel run {}", request.getProviderRunId());
        simulatedRunStore.cancel(request.getProviderRunId());
        return Response.ok().build();
    }

    @GET
    @Path("/status")
    public TaskStatusRes status(@QueryParam("providerRunId") String providerRunId) {
        log.info("Status of run {}", providerRunId);
        TaskStatusRes result = new TaskStatusRes();
        result.setProviderRunId(providerRunId);
        result.setStatus(simulatedRunStore.status(providerRunId));
        return result;
    }

    @GET
    @Path("/logs")
    public TaskLogsRes logs(@QueryParam("providerRunId") String providerRunId) {
        log.info("Logs of run {}", providerRunId);
        return simulatedRunStore.logs(providerRunId);
    }

    private List<String> secretHeaderNames(HttpHeaders headers) {
        List<String> names = new ArrayList<>();
        if (headers == null) {
            return names;
        }
        for (String name : headers.getRequestHeaders().keySet()) {
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(SECRET_HEADER_PREFIX)) {
                names.add(name);
            }
        }
        return names;
    }
}
