package org.executor.utils.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LifecycleLoggingObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleLoggingObserver.class);

    void onCrudLifecycle(@Observes(during = TransactionPhase.AFTER_SUCCESS) CrudLifecycleEvent event) {
        LOGGER.debug("CRUD lifecycle event {}", event);
    }

    void onEntityChange(@Observes(during = TransactionPhase.AFTER_SUCCESS) EntityChangeEvent event) {
        Object entity = event.getEntity();
        LOGGER.debug("Entity change type={} entity={}", event.getType(), entity == null ? null : entity.getClass().getName());
    }
}
