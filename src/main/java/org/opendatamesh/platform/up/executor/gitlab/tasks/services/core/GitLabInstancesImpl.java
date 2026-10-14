package org.opendatamesh.platform.up.executor.gitlab.tasks.services.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.gitlab.config.GitLabExecutorProperties;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;

import java.util.Optional;

@ApplicationScoped
public class GitLabInstancesImpl implements GitLabInstances {

    private final GitLabExecutorProperties properties;

    public GitLabInstancesImpl(GitLabExecutorProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<GitLabInstance> resolve(String providerBaseUrl) {
        return isBlank(providerBaseUrl) ? resolveDefault() : resolveAllowed(providerBaseUrl);
    }

    private Optional<GitLabInstance> resolveDefault() {
        return properties.defaultBaseUrl().flatMap(GitLabInstance::parse);
    }

    private Optional<GitLabInstance> resolveAllowed(String providerBaseUrl) {
        return GitLabInstance.parse(providerBaseUrl).filter(this::isAllowed);
    }

    private boolean isAllowed(GitLabInstance instance) {
        return properties.allowedBaseUrls().stream()
                .map(GitLabInstance::parse)
                .flatMap(Optional::stream)
                .anyMatch(allowed -> allowed.equals(instance));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
