package org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

@RegisterForReflection
public record GitLabGraphQlReq(
        @JsonProperty("query") String query,
        @JsonProperty("variables") Map<String, Object> variables
) {
}
