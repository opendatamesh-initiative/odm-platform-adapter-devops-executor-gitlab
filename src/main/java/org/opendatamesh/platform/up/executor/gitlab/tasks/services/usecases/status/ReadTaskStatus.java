package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.gitlab.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineState;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.RunStatus;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

class ReadTaskStatus implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReadTaskStatus.class);

    private static final Set<String> KNOWN_GITLAB_STATUSES = Set.of(
            "created", "waiting_for_resource", "preparing", "waiting_for_callback", "pending", "running",
            "scheduled", "canceling", "manual", "success", "failed", "skipped", "canceled");

    private final ReadTaskStatusCommand command;
    private final ReadTaskStatusPresenter presenter;
    private final ReadTaskStatusInstanceOutboundPort instancePort;
    private final ReadTaskStatusGitLabOutboundPort gitLabPort;

    ReadTaskStatus(ReadTaskStatusCommand command, ReadTaskStatusPresenter presenter,
                   ReadTaskStatusInstanceOutboundPort instancePort, ReadTaskStatusGitLabOutboundPort gitLabPort) {
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
        warnIfUnknownStatus(state);
        presenter.presentStatus(command.pipelineId(), RunStatus.fromGitLab(state.gitLabStatus()));
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

    private void warnIfUnknownStatus(PipelineState state) {
        if (!isKnownStatus(state.gitLabStatus())) {
            logger.warn("Unknown GitLab pipeline status {} for pipeline {}", state.gitLabStatus(), command.pipelineId());
        }
    }

    private static boolean isKnownStatus(String gitLabStatus) {
        return gitLabStatus != null && KNOWN_GITLAB_STATUSES.contains(gitLabStatus);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
