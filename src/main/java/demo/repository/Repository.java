package demo.repository;

import miniORM.core.EntityManager;

import java.util.List;

public class Repository<T> {
    private final Class<T> clazz;
    private final EntityManager entityManager;

    public Repository(Class<T> clazz, EntityManager entityManager) {
        this.clazz = clazz;
        this.entityManager = entityManager;
    }

    public void save(T entity) {
        entityManager.save(entity);
    }

    public T findById(Object id) {
        return entityManager.findById(clazz, id);
    }

    public List<T> findAll() {
        return entityManager.findAll(clazz);
    }

    public void update(T entity) {
        entityManager.update(entity);
    }

    public void delete(Object id) {
        entityManager.delete(clazz, id);
    }
}
