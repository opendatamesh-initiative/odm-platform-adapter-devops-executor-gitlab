package org.opendatamesh.platform.up.executor.gitlab.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class GitLabExecutorConfigValidator {

    private static final String ALLOWED_KEY = "executor.gitlab.allowed-base-urls";

    private final GitLabExecutorProperties properties;

    public GitLabExecutorConfigValidator(GitLabExecutorProperties properties) {
        this.properties = properties;
    }

    void onStart(@Observes StartupEvent event) {
        Set<String> allowed = validateAllowedBaseUrls(properties.allowedBaseUrls());
        validateDefaultBaseUrl(allowed);
        validateHttp();
        validateInputSpec();
        validateLogs();
    }

    private Set<String> validateAllowedBaseUrls(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalStateException(ALLOWED_KEY + ": at least one entry is required");
        }
        return entries.stream()
                .map(this::parseAllowedEntry)
                .map(GitLabInstance::baseUrl)
                .collect(Collectors.toSet());
    }

    private GitLabInstance parseAllowedEntry(String entry) {
        return GitLabInstance.parse(entry).orElseThrow(() -> new IllegalStateException(
                ALLOWED_KEY + ": entry is not an absolute http or https URL with a host"));
    }

    private void validateDefaultBaseUrl(Set<String> allowed) {
        properties.defaultBaseUrl().ifPresent(defaultUrl -> {
            String normalised = GitLabInstance.parse(defaultUrl)
                    .orElseThrow(() -> new IllegalStateException(
                            "executor.gitlab.default-base-url: value is not an absolute http or https URL with a host"))
                    .baseUrl();
            if (!allowed.contains(normalised)) {
                throw new IllegalStateException("executor.gitlab.default-base-url: value is not in executor.gitlab.allowed-base-urls");
            }
        });
    }

    private void validateHttp() {
        if (properties.http().readRetries() < 0) {
            throw new IllegalStateException("executor.gitlab.http.read-retries: must be >= 0");
        }
    }

    private void validateInputSpec() {
        requirePositive(properties.inputSpec().cacheMissRetryInterval(), "executor.gitlab.input-spec.cache-miss-retry-interval");
        requirePositive(properties.inputSpec().cacheMissBudget(), "executor.gitlab.input-spec.cache-miss-budget");
    }

    private void validateLogs() {
        requirePositiveSize(properties.logs().maxJobLogSize().asLongValue(), "executor.gitlab.logs.max-job-log-size");
        requirePositiveSize(properties.logs().maxTotalSize().asLongValue(), "executor.gitlab.logs.max-total-size");
    }

    private void requirePositive(Duration duration, String key) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalStateException(key + ": must be positive");
        }
    }

    private void requirePositiveSize(long bytes, String key) {
        if (bytes <= 0) {
            throw new IllegalStateException(key + ": must be positive");
        }
    }
}
