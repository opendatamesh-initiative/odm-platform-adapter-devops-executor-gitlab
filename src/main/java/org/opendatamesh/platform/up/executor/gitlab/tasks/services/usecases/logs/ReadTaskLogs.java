package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.gitlab.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCase;

import java.time.Instant;

class ReadTaskLogs implements UseCase {

    private final ReadTaskLogsCommand command;
    private final ReadTaskLogsPresenter presenter;
    private final ReadTaskLogsInstanceOutboundPort instancePort;
    private final ReadTaskLogsGitLabOutboundPort gitLabPort;

    ReadTaskLogs(ReadTaskLogsCommand command, ReadTaskLogsPresenter presenter,
                 ReadTaskLogsInstanceOutboundPort instancePort, ReadTaskLogsGitLabOutboundPort gitLabPort) {
        this.command = command;
        this.presenter = presenter;
        this.instancePort = instancePort;
        this.gitLabPort = gitLabPort;
    }

    @Override
    public void execute() {
        validateCommand();
        GitLabAccess access = resolveAccess();
        String content = gitLabPort.renderPipelineLogs(access, command.pipelineId());
        presenter.presentLogs(content, Instant.now());
    }

    private void validateCommand() {
        PipelineLocation location = command.location();
        if (isBlank(location.projectId())) {
            throw new BadRequestException("executorParameters.dataProductRepo.externalIdentifier is required");
        }
        if (isBlank(command.pipelineId())) {
            throw new BadRequestException("providerRunId is required");
        }
        if (isBlank(command.token())) {
            throw new BadRequestException("Header x-odm-token is required");
        }
    }

    private GitLabAccess resolveAccess() {
        PipelineLocation location = command.location();
        GitLabInstance instance = instancePort.resolveInstance(location.providerBaseUrl())
                .orElseThrow(() -> new BadRequestException(
                        "GitLab instance " + location.providerBaseUrl() + " is not allowed"));
        return new GitLabAccess(instance, location.projectId(), command.token());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
