package org.opendatamesh.platform.up.executor.gitlab.client.gitlab.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

@RegisterForReflection
public record GitLabCreatePipelineReq(
        @JsonProperty("ref") String ref,
        @JsonProperty("inputs") @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, Object> inputs
) {
}
