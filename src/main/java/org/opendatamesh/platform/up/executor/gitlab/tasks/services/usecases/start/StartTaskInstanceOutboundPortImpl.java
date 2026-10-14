package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.core.GitLabInstances;

import java.util.Optional;

class StartTaskInstanceOutboundPortImpl implements StartTaskInstanceOutboundPort {

    private final GitLabInstances instances;

    StartTaskInstanceOutboundPortImpl(GitLabInstances instances) {
        this.instances = instances;
    }

    @Override
    public Optional<GitLabInstance> resolveInstance(String providerBaseUrl) {
        return instances.resolve(providerBaseUrl);
    }
}
