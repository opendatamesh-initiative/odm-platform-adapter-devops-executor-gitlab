package org.opendatamesh.platform.up.executor.gitlab.tasks.services.core;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;

import java.util.Optional;

public interface GitLabInstances {

    Optional<GitLabInstance> resolve(String providerBaseUrl);
}
