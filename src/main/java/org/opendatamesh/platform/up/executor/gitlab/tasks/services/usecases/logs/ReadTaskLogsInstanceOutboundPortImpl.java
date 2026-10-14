package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.core.GitLabInstances;

import java.util.Optional;

class ReadTaskLogsInstanceOutboundPortImpl implements ReadTaskLogsInstanceOutboundPort {

    private final GitLabInstances instances;

    ReadTaskLogsInstanceOutboundPortImpl(GitLabInstances instances) {
        this.instances = instances;
    }

    @Override
    public Optional<GitLabInstance> resolveInstance(String providerBaseUrl) {
        return instances.resolve(providerBaseUrl);
    }
}
