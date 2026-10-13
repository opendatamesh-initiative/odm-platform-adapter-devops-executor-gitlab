package org.executor.utils.services.programmatic;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.executor.exceptions.InternalException;
import org.hibernate.Hibernate;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * Initializes a persistence graph far enough to use it after the persistence context closes, then detaches it.
 * <p>
 * Limits, matching the original algorithm:
 * <ul>
 *     <li>A null root returns immediately and is not detached.</li>
 *     <li>Only fields declared on the runtime class are visited. Inherited fields, including collections
 *     on a mapped superclass, are ignored.</li>
 *     <li>{@code Map} values are not {@link Collection}s, so they are not walked.</li>
 *     <li>Singular associations ({@code @ManyToOne}, {@code @OneToOne}) are not initialized unless something
 *     else already touched them. Only collection fields are recursed into.</li>
 *     <li>{@link IllegalAccessException} is wrapped in {@link InternalException}.</li>
 * </ul>
 * {@code @RegisterForReflection} on an entity does not by itself expose arbitrary new entities to native-image
 * reflection. Annotate every new entity before this walk runs in a native executable.
 * <p>
 * Registered only when {@code quarkus.hibernate-orm.active} is {@code true}.
 */
@IfBuildProperty(name = "quarkus.hibernate-orm.active", stringValue = "true")
@ApplicationScoped
public class EntityInitAndDetachServiceImpl implements EntityInitAndDetachService {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public void initializeEntityAndDetach(Object entity) {
        if (entity == null) {
            return;
        }
        initializeEntity(entity);
        entityManager.detach(entity);
    }

    private void initializeEntity(Object entity) {
        if (entity == null) {
            return;
        }
        Hibernate.initialize(entity);
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    Collection<?> collection = (Collection<?>) field.get(entity);
                    if (collection != null) {
                        collection.forEach(this::initializeEntity);
                    }
                } catch (IllegalAccessException exception) {
                    throw new InternalException(exception.getMessage(), exception);
                }
            }
        }
    }
}
