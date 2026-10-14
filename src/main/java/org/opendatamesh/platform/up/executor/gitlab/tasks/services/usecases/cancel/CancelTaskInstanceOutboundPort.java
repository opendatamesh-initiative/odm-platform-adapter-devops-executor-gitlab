package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;

import java.util.Optional;

interface CancelTaskInstanceOutboundPort {

    Optional<GitLabInstance> resolveInstance(String providerBaseUrl);
}
