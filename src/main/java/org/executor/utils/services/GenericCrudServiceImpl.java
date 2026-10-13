package org.executor.utils.services;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import org.executor.exceptions.BadRequestException;
import org.executor.exceptions.NotFoundException;
import org.executor.utils.aspects.GenericCrudIntercepted;
import org.executor.utils.repositories.Page;
import org.executor.utils.repositories.PageRequest;
import org.executor.utils.repositories.PagingAndSortingAndSpecificationExecutorRepository;
import org.executor.utils.repositories.Specification;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Function;

@GenericCrudIntercepted
public abstract class GenericCrudServiceImpl<T, ID extends Serializable> implements GenericCrudService<T, ID> {

    @Inject
    TransactionHandler transactionHandler;

    @Inject
    PageableSortValidator pageableSortValidator;

    @Inject
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    private Class<T> entityClass;

    @Override
    public Page<T> findAll(PageRequest pageable) {
        return findAll(null, pageable);
    }

    protected Page<T> findAll(Specification<T> spec, PageRequest pageable) {
        return transactionHandler.runInTransaction(() -> loadPage(spec, pageable));
    }

    @Override
    public T findOne(ID identifier) {
        return transactionHandler.runInTransaction(() -> loadOne(identifier));
    }

    @Override
    public void checkExistenceOrThrow(ID identifier) {
        if (!exists(identifier)) {
            throw new NotFoundException("Resource with id=" + identifier + " not found");
        }
    }

    @Override
    @Transactional
    public T create(T objectToCreate) {
        validate(objectToCreate);
        reconcile(objectToCreate);
        beforeCreation(objectToCreate);
        T result = getRepository().save(objectToCreate);
        afterCreation(objectToCreate, result);
        runAfterCommit(() -> afterCreationCommit(result));
        return result;
    }

    @Override
    @Transactional
    public T overwrite(ID identifier, T objectToOverwrite) {
        validate(objectToOverwrite);
        checkExistenceOrThrow(identifier);
        reconcile(objectToOverwrite);
        beforeOverwrite(objectToOverwrite);
        T result = getRepository().save(objectToOverwrite);
        afterOverWrite(objectToOverwrite, result);
        runAfterCommit(() -> afterOverwriteCommit(result));
        return result;
    }

    @Override
    @Transactional
    public void delete(ID identifier) {
        checkExistenceOrThrow(identifier);
        beforeDelete(identifier);
        getRepository().deleteById(identifier);
        afterDelete(identifier);
    }

    @Override
    public <R> R deleteReturning(ID identifier, Function<T, R> mapper) {
        T entity = findOne(identifier);
        R resource = mapper.apply(entity);
        transactionalSelf().delete(identifier);
        afterDeleteCommit(entity);
        return resource;
    }

    protected void afterFindOne(T foundObject, ID identifier) {
    }

    protected void beforeCreation(T objectToCreate) {
    }

    protected void afterCreation(T objectToCreate, T result) {
    }

    protected void afterCreationCommit(T createdEntity) {
    }

    protected void beforeOverwrite(T objectToOverwrite) {
    }

    protected void afterOverWrite(T objectToOverwrite, T result) {
    }

    protected void afterOverwriteCommit(T overwrittenObject) {
    }

    protected void beforeDelete(ID identifier) {
    }

    protected void afterDelete(ID identifier) {
    }

    protected void afterDeleteCommit(T entity) {
    }

    protected boolean exists(ID identifier) {
        return getRepository().existsById(identifier);
    }

    protected Set<String> getAllowedSortProperties() {
        return null;
    }

    protected abstract PagingAndSortingAndSpecificationExecutorRepository<T, ID> getRepository();

    protected abstract void validate(T objectToValidate);

    protected abstract void reconcile(T objectToReconcile);

    protected Class<T> getEntityClass() {
        if (entityClass == null) {
            entityClass = EntityClassResolver.resolveEntityClass(getClass());
        }
        return entityClass;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected GenericCrudService<T, ID> transactionalSelf() {
        Class<?> type = getClass();
        while (type.getName().contains("_Subclass") || type.getName().contains("_ClientProxy")) {
            type = type.getSuperclass();
        }
        return (GenericCrudService<T, ID>) CDI.current().select((Class) type).get();
    }

    private Page<T> loadPage(Specification<T> spec, PageRequest pageable) {
        validatePageable(pageable);
        return getRepository().findAll(spec, pageable);
    }

    private T loadOne(ID identifier) {
        T result = getRepository().findById(identifier).orElse(null);
        if (result == null) {
            throw new NotFoundException("Resource with id=" + identifier + " not found");
        }
        afterFindOne(result, identifier);
        return result;
    }

    private void validatePageable(PageRequest pageable) {
        if (pageable == null) {
            throw new BadRequestException("pageable is required");
        }
        if (pageable.getPage() < 0 || pageable.getSize() <= 0) {
            throw new BadRequestException("page must be >= 0 and size must be > 0");
        }
        pageableSortValidator.validate(pageable, getEntityClass(), getAllowedSortProperties());
    }

    private void runAfterCommit(Runnable action) {
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int status) {
                if (status == Status.STATUS_COMMITTED) {
                    action.run();
                }
            }
        });
    }
}
