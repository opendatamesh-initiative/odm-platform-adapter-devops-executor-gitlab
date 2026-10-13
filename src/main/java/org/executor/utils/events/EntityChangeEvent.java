package org.executor.utils.events;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class EntityChangeEvent {

    @RegisterForReflection
    public enum Type {
        INSERT,
        UPDATE,
        DELETE
    }

    private final Object entity;
    private final Type type;

    public EntityChangeEvent(Object entity, Type type) {
        this.entity = entity;
        this.type = type;
    }

    public Object getEntity() {
        return entity;
    }

    public Type getType() {
        return type;
    }
}
