package org.opendatamesh.platform.up.executor.gitlab.testsupport;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Starts one {@link GitLabStub} for the whole Quarkus test run and points the executor's allow-list and default
 * instance at it. Tests reach the stub through {@link #stub()}.
 */
public class GitLabStubResource implements QuarkusTestResourceLifecycleManager {

    private static volatile GitLabStub stub;

    public static GitLabStub stub() {
        GitLabStub current = stub;
        if (current == null) {
            throw new IllegalStateException("GitLabStubResource is not running");
        }
        return current;
    }

    @Override
    public Map<String, String> start() {
        try {
            stub = new GitLabStub();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start the GitLab stub", e);
        }
        String url = stub.baseUrl();
        return Map.of(
                "executor.gitlab.allowed-base-urls", url,
                "executor.gitlab.default-base-url", url);
    }

    @Override
    public void stop() {
        GitLabStub current = stub;
        stub = null;
        if (current != null) {
            current.close();
        }
    }
}
