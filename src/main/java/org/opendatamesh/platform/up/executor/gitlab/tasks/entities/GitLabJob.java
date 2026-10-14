package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

public record GitLabJob(long id, String name, String stage, String status) {
}
