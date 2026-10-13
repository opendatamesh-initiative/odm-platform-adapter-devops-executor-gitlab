package org.executor.utils.services.programmatic;

public interface EntityInitAndDetachService {

    /**
     * Initializes declared collection fields and detaches the entity.
     * A null root is a no-op. See {@link EntityInitAndDetachServiceImpl} for the limits.
     */
    void initializeEntityAndDetach(Object entity);
}
