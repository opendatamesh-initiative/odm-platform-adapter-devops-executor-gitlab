package org.executor.utils.services;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

final class EntityClassResolver {

    private EntityClassResolver() {
    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> resolveEntityClass(Class<?> serviceClass) {
        Class<?> clazz = serviceClass;
        while (clazz != null) {
            Type genericSuperclass = clazz.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType parameterizedType) {
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class<?> rawClass) {
                    if (GenericCrudServiceImpl.class.equals(rawClass)) {
                        return (Class<T>) parameterizedType.getActualTypeArguments()[0];
                    }
                    if (GenericMappedCrudServiceImpl.class.equals(rawClass)) {
                        return (Class<T>) parameterizedType.getActualTypeArguments()[1];
                    }
                    if (GenericMappedAndFilteredCrudServiceImpl.class.equals(rawClass)) {
                        return (Class<T>) parameterizedType.getActualTypeArguments()[2];
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        throw new IllegalStateException("Cannot resolve entity class for " + serviceClass.getName());
    }
}
