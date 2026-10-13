package org.executor.utils.database;

import org.executor.utils.events.EntityChangeEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

@RegisterForReflection
@ApplicationScoped
public class HibernateChangeDataCapture implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final Event<EntityChangeEvent> events;

    @Inject
    public HibernateChangeDataCapture(Event<EntityChangeEvent> events) {
        this.events = events;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        events.fire(new EntityChangeEvent(event.getEntity(), EntityChangeEvent.Type.INSERT));
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        events.fire(new EntityChangeEvent(event.getEntity(), EntityChangeEvent.Type.UPDATE));
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        events.fire(new EntityChangeEvent(event.getEntity(), EntityChangeEvent.Type.DELETE));
    }

    /**
     * After-commit delivery is done by CDI observers
     * ({@code @Observes(during = TransactionPhase.AFTER_SUCCESS)}), not by Hibernate post-commit hooks.
     */
    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }
}
