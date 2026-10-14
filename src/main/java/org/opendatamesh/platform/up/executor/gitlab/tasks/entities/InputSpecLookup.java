package org.opendatamesh.platform.up.executor.gitlab.tasks.entities;

import java.util.List;

public record InputSpecLookup(boolean ready, List<DeclaredInput> inputs) {

    public static InputSpecLookup notReady() {
        return new InputSpecLookup(false, List.of());
    }

    public static InputSpecLookup ready(List<DeclaredInput> inputs) {
        return new InputSpecLookup(true, List.copyOf(inputs));
    }
}
