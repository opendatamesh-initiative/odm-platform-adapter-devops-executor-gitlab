package org.opendatamesh.platform.up.executor.gitlab.config;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.TestExecutorProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitLabExecutorConfigValidatorTest {

    @Test
    void emptyAllowListIsRejected() {
        GitLabExecutorConfigValidator validator = new GitLabExecutorConfigValidator(
                TestExecutorProperties.of(null, List.of()));

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> validator.onStart(null));

        assertEquals("executor.gitlab.allowed-base-urls: at least one entry is required", failure.getMessage());
    }

    @Test
    void defaultOutsideTheAllowListIsRejected() {
        GitLabExecutorConfigValidator validator = new GitLabExecutorConfigValidator(
                TestExecutorProperties.of("https://gitlab.com", List.of("https://gitlab.example.org")));

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> validator.onStart(null));

        assertEquals("executor.gitlab.default-base-url: value is not in executor.gitlab.allowed-base-urls",
                failure.getMessage());
    }

    @Test
    void validConfigurationPasses() {
        GitLabExecutorConfigValidator validator = new GitLabExecutorConfigValidator(
                TestExecutorProperties.of("HTTPS://GitLab.com/", List.of("https://gitlab.com", "https://gitlab.example.org")));

        assertDoesNotThrow(() -> validator.onStart(null));
    }
}
