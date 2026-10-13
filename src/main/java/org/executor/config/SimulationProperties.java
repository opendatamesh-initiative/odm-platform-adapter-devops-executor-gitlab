package org.executor.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.time.Duration;

@ConfigMapping(prefix = "executor.simulation")
public interface SimulationProperties {

    @WithDefault("5s")
    Duration runDuration();

    @WithDefault("SUCCEEDED")
    String outcome();
}
