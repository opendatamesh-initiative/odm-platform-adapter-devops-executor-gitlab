package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.gitlab.client.gitlab.GitLabClient;
import org.opendatamesh.platform.up.executor.gitlab.config.GitLabExecutorProperties;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.core.GitLabInstances;
import org.opendatamesh.platform.up.executor.gitlab.utils.usecases.UseCase;

@ApplicationScoped
public class ReadTaskLogsFactory {

    private final GitLabClient client;
    private final GitLabInstances instances;
    private final GitLabExecutorProperties properties;

    public ReadTaskLogsFactory(GitLabClient client, GitLabInstances instances, GitLabExecutorProperties properties) {
        this.client = client;
        this.instances = instances;
        this.properties = properties;
    }

    public UseCase buildReadTaskLogs(ReadTaskLogsCommand command, ReadTaskLogsPresenter presenter) {
        return new ReadTaskLogs(command, presenter,
                new ReadTaskLogsInstanceOutboundPortImpl(instances),
                new ReadTaskLogsGitLabOutboundPortImpl(client,
                        properties.logs().maxJobLogSize().asLongValue(),
                        properties.logs().maxTotalSize().asLongValue()));
    }
}
