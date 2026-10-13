package org.executor.utils.services;

import org.executor.utils.repositories.Page;
import org.executor.utils.repositories.PageRequest;
import org.executor.utils.repositories.PagingAndSortingAndSpecificationExecutorRepository;
import org.executor.utils.repositories.Specification;

import java.io.Serializable;

public abstract class GenericMappedAndFilteredCrudServiceImpl<F, R, T, ID extends Serializable>
        extends GenericMappedCrudServiceImpl<R, T, ID>
        implements GenericMappedAndFilteredCrudService<F, R, T, ID> {

    @Override
    public Page<R> findAllResourcesFiltered(PageRequest pageable, F filters) {
        return transactionHandler.runInTransaction(() -> findAllFiltered(pageable, filters).map(this::toRes));
    }

    @Override
    public Page<T> findAllFiltered(PageRequest pageable, F filters) {
        Specification<T> spec = getSpecFromFilters(filters);
        return findAll(spec, pageable);
    }

    @Override
    protected abstract PagingAndSortingAndSpecificationExecutorRepository<T, ID> getRepository();

    protected abstract Specification<T> getSpecFromFilters(F filters);
}
