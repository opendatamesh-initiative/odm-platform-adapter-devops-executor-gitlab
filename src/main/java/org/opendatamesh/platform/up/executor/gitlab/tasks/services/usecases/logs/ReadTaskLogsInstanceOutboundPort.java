package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;

import java.util.Optional;

interface ReadTaskLogsInstanceOutboundPort {

    Optional<GitLabInstance> resolveInstance(String providerBaseUrl);
}
