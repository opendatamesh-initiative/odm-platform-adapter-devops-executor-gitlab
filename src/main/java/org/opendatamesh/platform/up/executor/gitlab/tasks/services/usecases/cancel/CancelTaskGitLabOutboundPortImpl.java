package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.GitLabClient;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineState;

class CancelTaskGitLabOutboundPortImpl implements CancelTaskGitLabOutboundPort {

    private final GitLabClient client;

    CancelTaskGitLabOutboundPortImpl(GitLabClient client) {
        this.client = client;
    }

    @Override
    public PipelineState getPipeline(GitLabAccess access, String pipelineId) {
        return client.getPipeline(access, pipelineId);
    }

    @Override
    public void cancelPipeline(GitLabAccess access, String pipelineId) {
        client.cancelPipeline(access, pipelineId);
    }
}
