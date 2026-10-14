package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.gitlab.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.ResourceConflictException;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineState;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.RunStatus;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CancelTask implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(CancelTask.class);

    private final CancelTaskCommand command;
    private final CancelTaskPresenter presenter;
    private final CancelTaskInstanceOutboundPort instancePort;
    private final CancelTaskGitLabOutboundPort gitLabPort;

    CancelTask(CancelTaskCommand command, CancelTaskPresenter presenter,
               CancelTaskInstanceOutboundPort instancePort, CancelTaskGitLabOutboundPort gitLabPort) {
        this.command = command;
        this.presenter = presenter;
        this.instancePort = instancePort;
        this.gitLabPort = gitLabPort;
    }

    @Override
    public void execute() {
        validateCommand();
        GitLabAccess access = resolveAccess();
        PipelineState state = gitLabPort.getPipeline(access, command.pipelineId());
        rejectEndedPipeline(state);
        cancelUnlessCanceling(access, state);
        logger.info("Cancel requested for GitLab pipeline {}", command.pipelineId());
        presenter.presentCancelRequested(command.pipelineId());
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

    private void rejectEndedPipeline(PipelineState state) {
        if (RunStatus.fromGitLab(state.gitLabStatus()).isTerminal()) {
            throw new ResourceConflictException("GitLab pipeline " + command.pipelineId()
                    + " has already ended with status " + state.gitLabStatus());
        }
    }

    private void cancelUnlessCanceling(GitLabAccess access, PipelineState state) {
        if (!RunStatus.isCanceling(state.gitLabStatus())) {
            gitLabPort.cancelPipeline(access, command.pipelineId());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
