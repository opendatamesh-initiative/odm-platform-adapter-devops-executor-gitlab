package org.opendatamesh.platform.up.executor.gitlab.tasks.services.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opendatamesh.platform.up.executor.gitlab.testsupport.TestExecutorProperties;
import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.GitLabInstance;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitLabInstancesImplTest {

    private static final String DEFAULT = "https://gitlab.com";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void blankUrlResolvesToTheDefaultInstance(String blank) {
        GitLabInstances instances = new GitLabInstancesImpl(
                TestExecutorProperties.of(DEFAULT, List.of(DEFAULT)));

        assertEquals(Optional.of(new GitLabInstance("https://gitlab.com")), instances.resolve(blank));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankUrlWithoutDefaultResolvesToEmpty(String blank) {
        GitLabInstances instances = new GitLabInstancesImpl(
                TestExecutorProperties.of(null, List.of(DEFAULT)));

        assertFalse(instances.resolve(blank).isPresent());
    }

    @Test
    void defaultIsNormalisedToo() {
        GitLabInstances instances = new GitLabInstancesImpl(
                TestExecutorProperties.of("HTTPS://GitLab.com/", List.of(DEFAULT)));

        assertEquals(Optional.of(new GitLabInstance("https://gitlab.com")), instances.resolve(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://gitlab.com",
            "HTTPS://GitLab.com/",
            "  https://gitlab.com/  ",
            "https://GITLAB.com"
    })
    void allowedUrlMatchesAfterNormalisation(String requested) {
        GitLabInstances instances = new GitLabInstancesImpl(
                TestExecutorProperties.of(DEFAULT, List.of(DEFAULT)));

        Optional<GitLabInstance> resolved = instances.resolve(requested);

        assertTrue(resolved.isPresent(), requested);
        assertEquals("https://gitlab.com", resolved.get().baseUrl());
    }

    @Test
    void allowListIsNormalisedBeforeMatching() {
        GitLabInstances instances = new GitLabInstancesImpl(
                TestExecutorProperties.of(null, List.of("HTTPS://GitLab.example.org/")));

        assertEquals(Optional.of(new GitLabInstance("https://gitlab.example.org")),
                instances.resolve("https://gitlab.example.org"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example.com",
            "http://gitlab.com",
            "https://gitlab.com.evil.example.com",
            "https://gitlab.com:8443"
    })
    void hostOutsideTheAllowListIsRefused(String requested) {
        GitLabInstances instances = new GitLabInstancesImpl(
                TestExecutorProperties.of(DEFAULT, List.of(DEFAULT)));

        assertFalse(instances.resolve(requested).isPresent(), requested);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not a url",
            "gitlab.com",
            "ftp://gitlab.com",
            "https://",
            "https://exa mple.com"
    })
    void unparsableOrNonHttpUrlIsRefused(String requested) {
        GitLabInstances instances = new GitLabInstancesImpl(
                TestExecutorProperties.of(DEFAULT, List.of(DEFAULT)));

        assertFalse(instances.resolve(requested).isPresent(), requested);
    }
}
