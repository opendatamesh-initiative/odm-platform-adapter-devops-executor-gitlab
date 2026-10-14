package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

public record GitLabAccess(GitLabInstance instance, String projectId, String token) {

    @Override
    public String toString() {
        return "GitLabAccess[instance=" + instance + ", projectId=" + projectId + ", token=***]";
    }
}
