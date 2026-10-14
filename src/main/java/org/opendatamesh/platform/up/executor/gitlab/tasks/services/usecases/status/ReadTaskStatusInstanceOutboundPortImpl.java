package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.core.GitLabInstances;

import java.util.Optional;

class ReadTaskStatusInstanceOutboundPortImpl implements ReadTaskStatusInstanceOutboundPort {

    private final GitLabInstances instances;

    ReadTaskStatusInstanceOutboundPortImpl(GitLabInstances instances) {
        this.instances = instances;
    }

    @Override
    public Optional<GitLabInstance> resolveInstance(String providerBaseUrl) {
        return instances.resolve(providerBaseUrl);
    }
}
