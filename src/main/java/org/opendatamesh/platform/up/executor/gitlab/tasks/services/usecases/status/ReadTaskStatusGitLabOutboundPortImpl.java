package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.GitLabClient;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineState;

class ReadTaskStatusGitLabOutboundPortImpl implements ReadTaskStatusGitLabOutboundPort {

    private final GitLabClient client;

    ReadTaskStatusGitLabOutboundPortImpl(GitLabClient client) {
        this.client = client;
    }

    @Override
    public PipelineState getPipeline(GitLabAccess access, String pipelineId) {
        return client.getPipeline(access, pipelineId);
    }
}
