package org.executor.utils.usecases;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;

import java.sql.Statement;
import java.util.function.Function;

/**
 * REQUIRED propagation ({@link QuarkusTransaction#joiningExisting()}), matching Spring
 * {@code TransactionTemplate}'s default.
 * <p>
 * Quarkus 3.40's {@code QuarkusTransaction} runner cannot set a transaction isolation level.
 * Serializable isolation is applied as the first statement on the enlisted PostgreSQL connection:
 * {@code SET TRANSACTION ISOLATION LEVEL SERIALIZABLE}. PostgreSQL rejects that statement if the
 * transaction has already executed SQL, so this method runs before the caller's work.
 * <p>
 * Registered only when {@code quarkus.hibernate-orm.active} is {@code true}.
 */
@IfBuildProperty(name = "quarkus.hibernate-orm.active", stringValue = "true")
@ApplicationScoped
public class DefaultTransactionalOutboundPortImpl implements TransactionalOutboundPort {

    private final EntityManager entityManager;

    @Inject
    public DefaultTransactionalOutboundPortImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void doInTransaction(Runnable runnable) {
        QuarkusTransaction.joiningExisting().run(() -> {
            applySerializableIsolation();
            runnable.run();
        });
    }

    @Override
    public <T, R> R doInTransactionWithResults(Function<T, R> function, T arg) {
        return QuarkusTransaction.joiningExisting().call(() -> {
            applySerializableIsolation();
            return function.apply(arg);
        });
    }

    private void applySerializableIsolation() {
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE");
            }
        });
    }
}
