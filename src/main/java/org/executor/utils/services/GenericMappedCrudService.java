package org.executor.utils.services;

import org.executor.utils.repositories.Page;
import org.executor.utils.repositories.PageRequest;

import java.io.Serializable;

public interface GenericMappedCrudService<R, T, ID extends Serializable> extends GenericCrudService<T, ID> {

    Page<R> findAllResources(PageRequest pageable);

    R findOneResource(ID identifier);

    R createResource(R objectToCreate);

    R overwriteResource(ID identifier, R objectToOverwrite);

    R deleteReturningResource(ID identifier);
}
