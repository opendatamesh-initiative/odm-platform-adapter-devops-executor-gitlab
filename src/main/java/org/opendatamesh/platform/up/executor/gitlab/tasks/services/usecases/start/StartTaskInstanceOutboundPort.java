package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;

import java.util.Optional;

interface StartTaskInstanceOutboundPort {

    Optional<GitLabInstance> resolveInstance(String providerBaseUrl);
}
