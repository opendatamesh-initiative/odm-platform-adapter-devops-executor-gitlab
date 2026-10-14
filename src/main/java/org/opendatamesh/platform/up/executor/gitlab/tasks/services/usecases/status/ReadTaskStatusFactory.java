package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.GitLabClient;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.core.GitLabInstances;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCase;

@ApplicationScoped
public class ReadTaskStatusFactory {

    private final GitLabClient client;
    private final GitLabInstances instances;

    public ReadTaskStatusFactory(GitLabClient client, GitLabInstances instances) {
        this.client = client;
        this.instances = instances;
    }

    public UseCase buildReadTaskStatus(ReadTaskStatusCommand command, ReadTaskStatusPresenter presenter) {
        return new ReadTaskStatus(command, presenter,
                new ReadTaskStatusInstanceOutboundPortImpl(instances),
                new ReadTaskStatusGitLabOutboundPortImpl(client));
    }
}
