package org.executor.utils.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class EntityManagerRepository<T, ID extends Serializable>
        implements PagingAndSortingAndSpecificationExecutorRepository<T, ID> {

    @PersistenceContext
    protected EntityManager entityManager;

    protected abstract Class<T> entityClass();

    protected String idAttributeName() {
        return "id";
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(entityManager.find(entityClass(), id));
    }

    @Override
    public boolean existsById(ID id) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(entityClass());
        criteriaQuery.select(criteriaBuilder.count(root));
        criteriaQuery.where(criteriaBuilder.equal(root.get(idAttributeName()), id));
        Long count = entityManager.createQuery(criteriaQuery).getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public Page<T> findAll(PageRequest pageable) {
        return findAll(null, pageable);
    }

    @Override
    public Page<T> findAll(Specification<T> specification, PageRequest pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass());
        Root<T> root = criteriaQuery.from(entityClass());
        applyWhere(specification, root, criteriaQuery, criteriaBuilder);
        applySort(pageable, root, criteriaQuery, criteriaBuilder);
        TypedQuery<T> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(pageable.getPage() * pageable.getSize());
        typedQuery.setMaxResults(pageable.getSize());
        List<T> content = typedQuery.getResultList();
        long total = count(specification);
        return new Page<>(content, pageable.getPage(), pageable.getSize(), total);
    }

    @Override
    public T save(T entity) {
        Object identifier = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
        if (identifier == null) {
            entityManager.persist(entity);
            entityManager.flush();
            return entity;
        }
        T merged = entityManager.merge(entity);
        entityManager.flush();
        return merged;
    }

    @Override
    public void deleteById(ID id) {
        T entity = entityManager.find(entityClass(), id);
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    private long count(Specification<T> specification) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(entityClass());
        criteriaQuery.select(criteriaBuilder.count(root));
        applyWhere(specification, root, criteriaQuery, criteriaBuilder);
        Long total = entityManager.createQuery(criteriaQuery).getSingleResult();
        return total == null ? 0L : total;
    }

    private void applyWhere(Specification<T> specification, Root<T> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
        if (specification == null) {
            return;
        }
        Predicate predicate = specification.toPredicate(root, criteriaQuery, criteriaBuilder);
        if (predicate != null) {
            criteriaQuery.where(predicate);
        }
    }

    private void applySort(PageRequest pageable, Root<T> root, CriteriaQuery<T> criteriaQuery, CriteriaBuilder criteriaBuilder) {
        if (pageable.getSort().isUnsorted()) {
            return;
        }
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort().getOrders()) {
            Path<?> path = resolvePath(root, order.getProperty());
            if (order.getDirection() == Sort.Direction.DESC) {
                orders.add(criteriaBuilder.desc(path));
            } else {
                orders.add(criteriaBuilder.asc(path));
            }
        }
        criteriaQuery.orderBy(orders);
    }

    private Path<?> resolvePath(Root<T> root, String property) {
        String[] parts = property.split("\\.");
        Path<?> path = root.get(parts[0]);
        for (int index = 1; index < parts.length; index++) {
            path = path.get(parts[index]);
        }
        return path;
    }
}
