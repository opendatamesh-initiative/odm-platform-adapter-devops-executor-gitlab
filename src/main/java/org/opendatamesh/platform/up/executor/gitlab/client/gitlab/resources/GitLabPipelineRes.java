package org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitLabPipelineRes(
        @JsonProperty("id") long id,
        @JsonProperty("status") String status
) {
}
