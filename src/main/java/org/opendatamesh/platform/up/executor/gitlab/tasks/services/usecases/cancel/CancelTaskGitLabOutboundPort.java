package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineState;

interface CancelTaskGitLabOutboundPort {

    PipelineState getPipeline(GitLabAccess access, String pipelineId);

    void cancelPipeline(GitLabAccess access, String pipelineId);
}
