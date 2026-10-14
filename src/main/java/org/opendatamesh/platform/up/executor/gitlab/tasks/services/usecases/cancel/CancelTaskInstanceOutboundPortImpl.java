package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;
import org.opendatamesh.platform.up.executor.gitlab.tasks.services.core.GitLabInstances;

import java.util.Optional;

class CancelTaskInstanceOutboundPortImpl implements CancelTaskInstanceOutboundPort {

    private final GitLabInstances instances;

    CancelTaskInstanceOutboundPortImpl(GitLabInstances instances) {
        this.instances = instances;
    }

    @Override
    public Optional<GitLabInstance> resolveInstance(String providerBaseUrl) {
        return instances.resolve(providerBaseUrl);
    }
}
