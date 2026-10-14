package org.opendatamesh.platform.up.executor.gitlab.testsupport;

import io.quarkus.runtime.configuration.MemorySize;
import org.opendatamesh.platform.up.executor.gitlab.config.GitLabExecutorProperties;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * In-memory {@link GitLabExecutorProperties} for unit tests, with the production defaults for everything except the
 * instance settings the test chooses.
 */
public final class TestExecutorProperties {

    private TestExecutorProperties() {
    }

    public static GitLabExecutorProperties of(String defaultBaseUrl, List<String> allowedBaseUrls) {
        Optional<String> defaultUrl = Optional.ofNullable(defaultBaseUrl);
        return new GitLabExecutorProperties() {
            @Override
            public Optional<String> defaultBaseUrl() {
                return defaultUrl;
            }

            @Override
            public List<String> allowedBaseUrls() {
                return allowedBaseUrls;
            }

            @Override
            public Http http() {
                return new Http() {
                    @Override
                    public Duration connectTimeout() {
                        return Duration.ofSeconds(5);
                    }

                    @Override
                    public int readRetries() {
                        return 2;
                    }
                };
            }

            @Override
            public InputSpec inputSpec() {
                return new InputSpec() {
                    @Override
                    public Duration cacheMissRetryInterval() {
                        return Duration.ofSeconds(1);
                    }

                    @Override
                    public Duration cacheMissBudget() {
                        return Duration.ofSeconds(20);
                    }
                };
            }

            @Override
            public Logs logs() {
                return new Logs() {
                    @Override
                    public MemorySize maxJobLogSize() {
                        return new MemorySize(BigInteger.valueOf(1024L * 1024));
                    }

                    @Override
                    public MemorySize maxTotalSize() {
                        return new MemorySize(BigInteger.valueOf(5L * 1024 * 1024));
                    }
                };
            }
        };
    }
}
