package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

public record PipelineState(String pipelineId, String gitLabStatus) {
}
