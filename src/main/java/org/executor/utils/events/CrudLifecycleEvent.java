package org.executor.utils.events;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum CrudLifecycleEvent {
    CREATED,
    OVERWRITTEN,
    DELETED
}
