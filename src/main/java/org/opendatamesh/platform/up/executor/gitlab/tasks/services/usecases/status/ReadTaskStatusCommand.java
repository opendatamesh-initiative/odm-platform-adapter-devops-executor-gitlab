package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;

public record ReadTaskStatusCommand(PipelineLocation location, String pipelineId, String token) {
}
