package org.opendatamesh.platform.up.executor.gitlab.tasks.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitRefRes {

    @Schema(description = "Name of the git ref (branch or tag) to run the pipeline on", example = "main")
    private String name;

    @Schema(description = "Type of the git ref; ignored by the GitLab executor")
    private String type;

    public GitRefRes() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
