package org.opendatamesh.platform.up.executor.gitlab.tasks.services;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.RunStatus;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.DataProductRepoRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.ExecutorParametersRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.GitRefRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskLogsRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskRunCommandRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskStartCommandRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskStartResultRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.resources.TaskStatusRes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel.CancelTaskCommand;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel.CancelTaskFactory;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel.CancelTaskPresenter;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs.ReadTaskLogsCommand;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs.ReadTaskLogsFactory;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs.ReadTaskLogsPresenter;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start.StartTaskCommand;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start.StartTaskFactory;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start.StartTaskPresenter;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status.ReadTaskStatusCommand;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status.ReadTaskStatusFactory;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status.ReadTaskStatusPresenter;

import java.time.Instant;
import java.util.Date;

@ApplicationScoped
public class ExecutorTaskUseCasesService {

    private static final String EXECUTOR_PARAMETERS_REQUIRED = "executorParameters is required";
    private static final String PROVIDER_RUN_ID_REQUIRED = "providerRunId is required";

    private final StartTaskFactory startTaskFactory;
    private final ReadTaskStatusFactory readTaskStatusFactory;
    private final ReadTaskLogsFactory readTaskLogsFactory;
    private final CancelTaskFactory cancelTaskFactory;

    public ExecutorTaskUseCasesService(StartTaskFactory startTaskFactory,
                                       ReadTaskStatusFactory readTaskStatusFactory,
                                       ReadTaskLogsFactory readTaskLogsFactory,
                                       CancelTaskFactory cancelTaskFactory) {
        this.startTaskFactory = startTaskFactory;
        this.readTaskStatusFactory = readTaskStatusFactory;
        this.readTaskLogsFactory = readTaskLogsFactory;
        this.cancelTaskFactory = cancelTaskFactory;
    }

    public TaskStartResultRes start(TaskStartCommandRes body, String token) {
        TaskStartCommandRes request = requireStart(body);
        StartTaskResultHolder holder = new StartTaskResultHolder();
        startTaskFactory.buildStartTask(
                new StartTaskCommand(toLocation(request.getExecutorParameters()), request.getPipelineParameters(), token),
                holder).execute();
        return startResult(holder);
    }

    public TaskStatusRes status(TaskRunCommandRes body, String token) {
        TaskRunCommandRes run = requireRun(body);
        ReadTaskStatusResultHolder holder = new ReadTaskStatusResultHolder();
        readTaskStatusFactory.buildReadTaskStatus(
                new ReadTaskStatusCommand(toLocation(run.getExecutorParameters()), run.getProviderRunId(), token),
                holder).execute();
        return statusResult(holder);
    }

    public TaskLogsRes logs(TaskRunCommandRes body, String token) {
        TaskRunCommandRes run = requireRun(body);
        ReadTaskLogsResultHolder holder = new ReadTaskLogsResultHolder();
        readTaskLogsFactory.buildReadTaskLogs(
                new ReadTaskLogsCommand(toLocation(run.getExecutorParameters()), run.getProviderRunId(), token),
                holder).execute();
        return logsResult(holder);
    }

    public void cancel(TaskRunCommandRes body, String token) {
        TaskRunCommandRes run = requireRun(body);
        cancelTaskFactory.buildCancelTask(
                new CancelTaskCommand(toLocation(run.getExecutorParameters()), run.getProviderRunId(), token),
                pipelineId -> { }).execute();
    }

    private static TaskStartCommandRes requireStart(TaskStartCommandRes body) {
        if (body == null || body.getExecutorParameters() == null) {
            throw new BadRequestException(EXECUTOR_PARAMETERS_REQUIRED);
        }
        return body;
    }

    private static TaskRunCommandRes requireRun(TaskRunCommandRes body) {
        if (body == null || body.getExecutorParameters() == null) {
            throw new BadRequestException(EXECUTOR_PARAMETERS_REQUIRED);
        }
        if (body.getProviderRunId() == null || body.getProviderRunId().isBlank()) {
            throw new BadRequestException(PROVIDER_RUN_ID_REQUIRED);
        }
        return body;
    }

    private static PipelineLocation toLocation(ExecutorParametersRes parameters) {
        DataProductRepoRes repo = parameters.getDataProductRepo();
        GitRefRes ref = parameters.getRef();
        return new PipelineLocation(
                repo == null ? null : repo.getProviderBaseUrl(),
                repo == null ? null : repo.getExternalIdentifier(),
                ref == null ? null : ref.getName());
    }

    private static TaskStartResultRes startResult(StartTaskResultHolder holder) {
        TaskStartResultRes res = new TaskStartResultRes();
        res.setProviderRunId(holder.pipelineId);
        return res;
    }

    private static TaskStatusRes statusResult(ReadTaskStatusResultHolder holder) {
        TaskStatusRes res = new TaskStatusRes();
        res.setProviderRunId(holder.pipelineId);
        res.setStatus(holder.status.name());
        return res;
    }

    private static TaskLogsRes logsResult(ReadTaskLogsResultHolder holder) {
        TaskLogsRes res = new TaskLogsRes();
        res.setContent(holder.content);
        res.setGeneratedAt(Date.from(holder.generatedAt));
        return res;
    }

    private static final class StartTaskResultHolder implements StartTaskPresenter {

        private String pipelineId;

        @Override
        public void presentStarted(String pipelineId) {
            this.pipelineId = pipelineId;
        }
    }

    private static final class ReadTaskStatusResultHolder implements ReadTaskStatusPresenter {

        private String pipelineId;
        private RunStatus status;

        @Override
        public void presentStatus(String pipelineId, RunStatus status) {
            this.pipelineId = pipelineId;
            this.status = status;
        }
    }

    private static final class ReadTaskLogsResultHolder implements ReadTaskLogsPresenter {

        private String content;
        private Instant generatedAt;

        @Override
        public void presentLogs(String content, Instant generatedAt) {
            this.content = content;
            this.generatedAt = generatedAt;
        }
    }
}
