package org.executor.utils.aspects;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * CDI equivalent of Spring {@code GenericCrudAspect}. Subclass, override the
 * {@code around*} methods, and register the subclass as an interceptor bound
 * with {@link GenericCrudIntercepted}.
 * <p>
 * Method groups match the Spring pointcuts: find, create, overwrite, delete.
 * Nested proxy calls from the same thread (for example {@code createResource}
 * delegating to {@code create} through {@code transactionalSelf()}) skip the
 * {@code around*} methods so advice runs once, as with Spring self-invocation.
 */
public abstract class GenericCrudInterceptor {

    private static final ThreadLocal<InvocationContext> CURRENT = new ThreadLocal<>();

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        InvocationContext outer = CURRENT.get();
        if (outer != null && outer != context) {
            return context.proceed();
        }
        boolean root = outer == null;
        if (root) {
            CURRENT.set(context);
        }
        try {
            return dispatch(context);
        } finally {
            if (root) {
                CURRENT.remove();
            }
        }
    }

    private Object dispatch(InvocationContext context) throws Exception {
        String name = context.getMethod().getName();
        if (isFind(name)) {
            return aroundFind(context);
        }
        if (isCreate(name)) {
            return aroundCreate(context);
        }
        if (isOverwrite(name)) {
            return aroundOverwrite(context);
        }
        if (isDelete(name)) {
            return aroundDelete(context);
        }
        return context.proceed();
    }

    protected Object aroundFind(InvocationContext context) throws Exception {
        return context.proceed();
    }

    protected Object aroundCreate(InvocationContext context) throws Exception {
        return context.proceed();
    }

    protected Object aroundOverwrite(InvocationContext context) throws Exception {
        return context.proceed();
    }

    protected Object aroundDelete(InvocationContext context) throws Exception {
        return context.proceed();
    }

    private static boolean isFind(String name) {
        return "findOne".equals(name)
                || "findAll".equals(name)
                || "findOneResource".equals(name)
                || "findAllResources".equals(name)
                || "findAllResourcesFiltered".equals(name)
                || "findAllFiltered".equals(name);
    }

    private static boolean isCreate(String name) {
        return "create".equals(name) || "createResource".equals(name);
    }

    private static boolean isOverwrite(String name) {
        return "overwrite".equals(name) || "overwriteResource".equals(name);
    }

    private static boolean isDelete(String name) {
        return "delete".equals(name)
                || "deleteReturning".equals(name)
                || "deleteReturningResource".equals(name);
    }
}
