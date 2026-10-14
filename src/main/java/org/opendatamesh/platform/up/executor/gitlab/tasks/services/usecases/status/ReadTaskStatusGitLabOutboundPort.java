package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineState;

interface ReadTaskStatusGitLabOutboundPort {

    PipelineState getPipeline(GitLabAccess access, String pipelineId);
}
