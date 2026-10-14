package org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitLabJobRes(
        @JsonProperty("id") long id,
        @JsonProperty("name") String name,
        @JsonProperty("stage") String stage,
        @JsonProperty("status") String status
) {
}
