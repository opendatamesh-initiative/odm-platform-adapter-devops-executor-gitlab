package org.opendatamesh.platform.up.executor.gitlab.tasks.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStartCommandRes {

    @Schema(description = "Executor parameters of the task", required = true)
    private ExecutorParametersRes executorParameters;

    @Schema(description = "Pipeline parameters, keyed by input name; only declared inputs are sent to GitLab")
    private Map<String, String> pipelineParameters;

    public TaskStartCommandRes() {
    }

    public ExecutorParametersRes getExecutorParameters() {
        return executorParameters;
    }

    public void setExecutorParameters(ExecutorParametersRes executorParameters) {
        this.executorParameters = executorParameters;
    }

    public Map<String, String> getPipelineParameters() {
        return pipelineParameters;
    }

    public void setPipelineParameters(Map<String, String> pipelineParameters) {
        this.pipelineParameters = pipelineParameters;
    }
}
