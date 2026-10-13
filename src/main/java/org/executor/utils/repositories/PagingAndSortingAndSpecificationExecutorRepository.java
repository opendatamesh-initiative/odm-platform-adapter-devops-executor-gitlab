package org.executor.utils.repositories;

import java.io.Serializable;
import java.util.Optional;

/**
 * EntityManager-backed repository port. Replaces Spring Data's combined
 * CrudRepository, PagingAndSortingRepository, and JpaSpecificationExecutor.
 */
public interface PagingAndSortingAndSpecificationExecutorRepository<T, ID extends Serializable> {

    Optional<T> findById(ID id);

    boolean existsById(ID id);

    Page<T> findAll(PageRequest pageable);

    Page<T> findAll(Specification<T> specification, PageRequest pageable);

    T save(T entity);

    void deleteById(ID id);
}
