package org.executor.utils.services;

import org.executor.utils.repositories.Page;
import org.executor.utils.repositories.PageRequest;

import java.io.Serializable;
import java.util.function.Function;

public interface GenericCrudService<T, ID extends Serializable> {

    Page<T> findAll(PageRequest pageable);

    T findOne(ID identifier);

    void checkExistenceOrThrow(ID identifier);

    T create(T objectToCreate);

    T overwrite(ID identifier, T objectToOverwrite);

    void delete(ID identifier);

    <R> R deleteReturning(ID identifier, Function<T, R> mapper);
}
