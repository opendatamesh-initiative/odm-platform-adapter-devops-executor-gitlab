package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;

public record CancelTaskCommand(PipelineLocation location, String pipelineId, String token) {
}
