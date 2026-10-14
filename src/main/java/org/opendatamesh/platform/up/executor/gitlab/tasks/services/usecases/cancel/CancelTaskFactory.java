package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.GitLabClient;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.core.GitLabInstances;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCase;

@ApplicationScoped
public class CancelTaskFactory {

    private final GitLabClient client;
    private final GitLabInstances instances;

    public CancelTaskFactory(GitLabClient client, GitLabInstances instances) {
        this.client = client;
        this.instances = instances;
    }

    public UseCase buildCancelTask(CancelTaskCommand command, CancelTaskPresenter presenter) {
        return new CancelTask(command, presenter,
                new CancelTaskInstanceOutboundPortImpl(instances),
                new CancelTaskGitLabOutboundPortImpl(client));
    }
}
