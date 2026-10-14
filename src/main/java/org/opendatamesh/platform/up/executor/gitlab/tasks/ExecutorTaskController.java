package org.opendatamesh.platform.up.executor.gitlab.tasks;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskLogsRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskRunCommandRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskStartCommandRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskStartResultRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskStatusRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.ExecutorTaskUseCasesService;

@Path("/api/v2/up/executor/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Executor tasks")
public class ExecutorTaskController {

    static final String TOKEN_HEADER = "x-odm-token";

    private final ExecutorTaskUseCasesService service;

    public ExecutorTaskController(ExecutorTaskUseCasesService service) {
        this.service = service;
    }

    @POST
    @Path("/start")
    @Operation(summary = "Start a GitLab CI/CD pipeline for a task",
            description = "Creates a pipeline on the task's project and ref. Only declared pipeline inputs are sent.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Pipeline created"),
            @APIResponse(responseCode = "400", description = "Invalid request, unknown or disallowed instance, or missing required inputs"),
            @APIResponse(responseCode = "404", description = "GitLab project or ref not found"),
            @APIResponse(responseCode = "502", description = "GitLab refused the token or failed"),
            @APIResponse(responseCode = "503", description = "Input specification of the ref still not available")
    })
    public TaskStartResultRes start(TaskStartCommandRes body, @HeaderParam(TOKEN_HEADER) String token) {
        return service.start(body, token);
    }

    @POST
    @Path("/status")
    @Operation(summary = "Read the status of a pipeline run")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Run status"),
            @APIResponse(responseCode = "400", description = "Invalid request or disallowed instance"),
            @APIResponse(responseCode = "404", description = "GitLab project or pipeline not found"),
            @APIResponse(responseCode = "502", description = "GitLab refused the token or failed")
    })
    public TaskStatusRes status(TaskRunCommandRes body, @HeaderParam(TOKEN_HEADER) String token) {
        return service.status(body, token);
    }

    @POST
    @Path("/logs")
    @Operation(summary = "Read the job logs of a pipeline run as one block")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Job logs"),
            @APIResponse(responseCode = "400", description = "Invalid request or disallowed instance"),
            @APIResponse(responseCode = "404", description = "GitLab project or pipeline not found"),
            @APIResponse(responseCode = "502", description = "GitLab refused the token or failed")
    })
    public TaskLogsRes logs(TaskRunCommandRes body, @HeaderParam(TOKEN_HEADER) String token) {
        return service.logs(body, token);
    }

    @POST
    @Path("/cancel")
    @Operation(summary = "Cancel a pipeline run")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Run canceled, or already canceling"),
            @APIResponse(responseCode = "400", description = "Invalid request or disallowed instance"),
            @APIResponse(responseCode = "404", description = "GitLab project or pipeline not found"),
            @APIResponse(responseCode = "409", description = "Pipeline already ended"),
            @APIResponse(responseCode = "502", description = "GitLab refused the token or failed")
    })
    public Response cancel(TaskRunCommandRes body, @HeaderParam(TOKEN_HEADER) String token) {
        service.cancel(body, token);
        return Response.ok().build();
    }
}
