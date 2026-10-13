package org.executor.utils.services;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import org.executor.exceptions.BadRequestException;
import org.executor.utils.repositories.PageRequest;
import org.executor.utils.repositories.Sort;

import java.util.Arrays;
import java.util.Set;

/**
 * Registered only when {@code quarkus.hibernate-orm.active} is {@code true}.
 */
@IfBuildProperty(name = "quarkus.hibernate-orm.active", stringValue = "true")
@ApplicationScoped
public class PageableSortValidator {

    @PersistenceContext
    EntityManager entityManager;

    public void validate(PageRequest pageable, Class<?> entityClass, Set<String> allowedSortProperties) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return;
        }
        for (Sort.Order order : pageable.getSort().getOrders()) {
            String property = order.getProperty();
            if (allowedSortProperties != null) {
                if (!allowedSortProperties.contains(property)) {
                    throw new BadRequestException("Sort property '" + property + "' not supported");
                }
            } else if (!isPersistentSortProperty(entityClass, property)) {
                throw new BadRequestException("Sort property '" + property + "' not supported");
            }
        }
    }

    private boolean isPersistentSortProperty(Class<?> entityClass, String propertyPath) {
        try {
            ManagedType<?> managedType = entityManager.getMetamodel().managedType(entityClass);
            return resolvePath(managedType, propertyPath.split("\\."));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean resolvePath(ManagedType<?> managedType, String[] parts) {
        if (parts.length == 0) {
            return false;
        }
        String part = parts[0];
        if (managedType.getAttributes().stream().noneMatch(attribute -> attribute.getName().equals(part))) {
            return false;
        }
        Attribute<?, ?> attribute = managedType.getAttribute(part);
        if (parts.length == 1) {
            return true;
        }
        if (attribute.isAssociation() || attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED) {
            ManagedType<?> nestedType = entityManager.getMetamodel().managedType(attribute.getJavaType());
            return resolvePath(nestedType, Arrays.copyOfRange(parts, 1, parts.length));
        }
        return false;
    }
}
