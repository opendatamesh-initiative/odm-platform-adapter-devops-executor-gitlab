package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;

interface ReadTaskLogsGitLabOutboundPort {

    String renderPipelineLogs(GitLabAccess access, String pipelineId);
}
