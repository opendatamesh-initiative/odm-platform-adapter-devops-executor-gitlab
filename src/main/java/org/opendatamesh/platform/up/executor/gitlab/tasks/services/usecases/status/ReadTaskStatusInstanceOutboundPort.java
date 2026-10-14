package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;

import java.util.Optional;

interface ReadTaskStatusInstanceOutboundPort {

    Optional<GitLabInstance> resolveInstance(String providerBaseUrl);
}
