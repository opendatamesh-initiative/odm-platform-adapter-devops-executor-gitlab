package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

public record PipelineLocation(String providerBaseUrl, String projectId, String refName) {
}
