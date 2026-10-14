package org.opendatamesh.platform.up.executor.gitlab.tasks.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStatusRes {

    @Schema(description = "GitLab pipeline id as a decimal string")
    private String providerRunId;

    @Schema(description = "Run status", enumeration = {"RUNNING", "SUCCEEDED", "FAILED", "CANCELED"})
    private String status;

    public TaskStatusRes() {
    }

    public String getProviderRunId() {
        return providerRunId;
    }

    public void setProviderRunId(String providerRunId) {
        this.providerRunId = providerRunId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
