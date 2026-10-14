package org.opendatamesh.platform.up.executor.gitlab.config;

import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "executor.gitlab")
public interface GitLabExecutorProperties {

    Optional<String> defaultBaseUrl();

    List<String> allowedBaseUrls();

    Http http();

    InputSpec inputSpec();

    Logs logs();

    interface Http {

        @WithDefault("5s")
        Duration connectTimeout();

        @WithDefault("2")
        int readRetries();
    }

    interface InputSpec {

        @WithDefault("1s")
        Duration cacheMissRetryInterval();

        @WithDefault("20s")
        Duration cacheMissBudget();
    }

    interface Logs {

        @WithDefault("1M")
        MemorySize maxJobLogSize();

        @WithDefault("5M")
        MemorySize maxTotalSize();
    }
}
