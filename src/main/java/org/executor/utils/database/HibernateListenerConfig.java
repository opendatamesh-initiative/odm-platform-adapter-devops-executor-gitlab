package org.executor.utils.database;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;

/**
 * Registered only when {@code quarkus.hibernate-orm.active} is {@code true}.
 */
@IfBuildProperty(name = "quarkus.hibernate-orm.active", stringValue = "true")
@ApplicationScoped
public class HibernateListenerConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final HibernateChangeDataCapture listener;

    @Inject
    public HibernateListenerConfig(EntityManagerFactory entityManagerFactory, HibernateChangeDataCapture listener) {
        this.entityManagerFactory = entityManagerFactory;
        this.listener = listener;
    }

    @PostConstruct
    public void registerListeners() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry()
                .getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(listener);
        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(listener);
    }
}
