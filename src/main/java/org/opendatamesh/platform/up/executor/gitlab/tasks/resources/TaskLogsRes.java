package org.opendatamesh.platform.up.executor.gitlab.tasks.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskLogsRes {

    @Schema(description = "Job logs of the pipeline, as one block")
    private String content;

    @Schema(description = "Time the logs were generated")
    private Date generatedAt;

    public TaskLogsRes() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }
}
