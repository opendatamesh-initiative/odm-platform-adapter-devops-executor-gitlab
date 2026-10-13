package org.executor.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStartCommandRes {

    private ExecutorParametersRes executorParameters;

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
