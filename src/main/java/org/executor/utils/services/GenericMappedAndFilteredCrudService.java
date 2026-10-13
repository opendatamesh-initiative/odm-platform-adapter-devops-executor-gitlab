package org.executor.utils.services;

import org.executor.utils.repositories.Page;
import org.executor.utils.repositories.PageRequest;

import java.io.Serializable;

public interface GenericMappedAndFilteredCrudService<F, R, T, ID extends Serializable> extends GenericMappedCrudService<R, T, ID> {

    Page<R> findAllResourcesFiltered(PageRequest pageable, F filters);

    Page<T> findAllFiltered(PageRequest pageable, F filters);
}
