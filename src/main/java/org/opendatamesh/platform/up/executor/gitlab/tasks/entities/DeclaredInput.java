package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

public record DeclaredInput(String name, InputType type, boolean required) {
}
