package org.executor.utils.aspects;

import org.executor.utils.events.CrudLifecycleEvent;
import io.quarkus.arc.Unremovable;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Unremovable
@GenericCrudIntercepted
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class CrudLifecycleInterceptor extends GenericCrudInterceptor {

    private final Event<CrudLifecycleEvent> lifecycleEvents;

    @Inject
    public CrudLifecycleInterceptor(Event<CrudLifecycleEvent> lifecycleEvents) {
        this.lifecycleEvents = lifecycleEvents;
    }

    @Override
    protected Object aroundCreate(InvocationContext context) throws Exception {
        Object result = context.proceed();
        lifecycleEvents.fire(CrudLifecycleEvent.CREATED);
        return result;
    }

    @Override
    protected Object aroundOverwrite(InvocationContext context) throws Exception {
        Object result = context.proceed();
        lifecycleEvents.fire(CrudLifecycleEvent.OVERWRITTEN);
        return result;
    }

    @Override
    protected Object aroundDelete(InvocationContext context) throws Exception {
        Object result = context.proceed();
        lifecycleEvents.fire(CrudLifecycleEvent.DELETED);
        return result;
    }
}
