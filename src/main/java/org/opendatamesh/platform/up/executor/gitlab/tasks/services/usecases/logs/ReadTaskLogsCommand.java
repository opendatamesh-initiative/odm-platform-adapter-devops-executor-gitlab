package org.opendatamesh.platform.up.executor.gitlab.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.gitlab.tasks.entities.PipelineLocation;

public record ReadTaskLogsCommand(PipelineLocation location, String pipelineId, String token) {
}
