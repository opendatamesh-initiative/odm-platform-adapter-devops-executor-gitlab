package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.GitLabClient;
import org.opendatamesh.platform.up.executor.gitlab.exceptions.InternalException;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabAccess;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.InputSpecLookup;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.SelectedInput;

import java.time.Duration;
import java.util.List;

class StartTaskGitLabOutboundPortImpl implements StartTaskGitLabOutboundPort {

    private final GitLabClient client;
    private final Duration retryInterval;
    private final Duration budget;

    StartTaskGitLabOutboundPortImpl(GitLabClient client, Duration retryInterval, Duration budget) {
        this.client = client;
        this.retryInterval = retryInterval;
        this.budget = budget;
    }

    @Override
    public String findProjectPath(GitLabAccess access) {
        return client.findProjectPath(access);
    }

    @Override
    public InputSpecLookup findDeclaredInputs(GitLabAccess access, String projectPath, String ref) {
        return client.findDeclaredInputs(access, projectPath, ref);
    }

    @Override
    public void waitBeforeNextInputSpecRead() {
        try {
            Thread.sleep(retryInterval.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalException("Interrupted while waiting for the pipeline inputs", e);
        }
    }

    @Override
    public int maxInputSpecReads() {
        long reads = budget.toMillis() / retryInterval.toMillis();
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, reads));
    }

    @Override
    public String createPipeline(GitLabAccess access, String ref, List<SelectedInput> inputs) {
        return client.createPipeline(access, ref, inputs);
    }
}
