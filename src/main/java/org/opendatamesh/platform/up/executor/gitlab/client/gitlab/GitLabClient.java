package org.opendatamesh.platform.up.executor.gitlab.client.gitlab;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabJob;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.InputSpecLookup;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineState;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.SelectedInput;

import java.util.List;
import java.util.Optional;

public interface GitLabClient {

    String findProjectPath(GitLabAccess access);

    InputSpecLookup findDeclaredInputs(GitLabAccess access, String projectPath, String ref);

    String createPipeline(GitLabAccess access, String ref, List<SelectedInput> inputs);

    PipelineState getPipeline(GitLabAccess access, String pipelineId);

    void cancelPipeline(GitLabAccess access, String pipelineId);

    List<GitLabJob> listJobs(GitLabAccess access, String pipelineId);

    Optional<String> readJobLog(GitLabAccess access, long jobId);
}
