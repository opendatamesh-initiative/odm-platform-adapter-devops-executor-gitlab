package org.executor.utils.services;

import org.executor.utils.repositories.Page;
import org.executor.utils.repositories.PageRequest;

import java.io.Serializable;

public abstract class GenericMappedCrudServiceImpl<R, T, ID extends Serializable>
        extends GenericCrudServiceImpl<T, ID>
        implements GenericMappedCrudService<R, T, ID> {

    protected abstract R toRes(T entity);

    protected abstract T toEntity(R resource);

    @Override
    public Page<R> findAllResources(PageRequest pageable) {
        return transactionHandler.runInTransaction(() -> findAll(pageable).map(this::toRes));
    }

    @Override
    public R findOneResource(ID identifier) {
        return transactionHandler.runInTransaction(() -> toRes(findOne(identifier)));
    }

    @Override
    public R createResource(R objectToCreate) {
        T createdEntity = transactionalSelf().create(toEntity(objectToCreate));
        return toRes(createdEntity);
    }

    @Override
    public R overwriteResource(ID identifier, R objectToOverwrite) {
        T overwrittenEntity = transactionalSelf().overwrite(identifier, toEntity(objectToOverwrite));
        return toRes(overwrittenEntity);
    }

    @Override
    public R deleteReturningResource(ID identifier) {
        return deleteReturning(identifier, this::toRes);
    }
}
