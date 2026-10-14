package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.GitLabClient;
import org.opendatamesh.platform.up.executor.gitlab.config.GitLabExecutorProperties;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.core.GitLabInstances;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCase;

@ApplicationScoped
public class StartTaskFactory {

    private final GitLabClient client;
    private final GitLabInstances instances;
    private final GitLabExecutorProperties properties;

    public StartTaskFactory(GitLabClient client, GitLabInstances instances, GitLabExecutorProperties properties) {
        this.client = client;
        this.instances = instances;
        this.properties = properties;
    }

    public UseCase buildStartTask(StartTaskCommand command, StartTaskPresenter presenter) {
        return new StartTask(command, presenter,
                new StartTaskInstanceOutboundPortImpl(instances),
                new StartTaskGitLabOutboundPortImpl(client,
                        properties.inputSpec().cacheMissRetryInterval(),
                        properties.inputSpec().cacheMissBudget()));
    }
}
