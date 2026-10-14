package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;

import java.util.Map;

public record StartTaskCommand(PipelineLocation location, Map<String, String> pipelineParameters, String token) {
}
