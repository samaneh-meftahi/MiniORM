package miniORM.core;

import miniORM.annotation.Relation.JoinColumn;
import miniORM.annotation.Relation.ManyToOne;
import miniORM.annotation.Relation.OneToOne;
import miniORM.metaData.EntityMetaData;
import miniORM.sql.SQLGenerator;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityManager {

    private final DataSource dataSource;
    private final Map<Class<?>, EntityMetaData> metaDataCache = new ConcurrentHashMap<>();

    public EntityManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private EntityMetaData getMetaData(Class<?> clazz) {
        return metaDataCache.computeIfAbsent(clazz, EntityMetaData::new);
    }

    public <T> void save(T entity) {
        EntityMetaData metaData = getMetaData(entity.getClass());
        String sql = SQLGenerator.buildInsertQuery(metaData);

        try (Connection connection = dataSource.getConnection()) {
            TransactionManager tx = new TransactionManager(connection);
            tx.begin();

            boolean hasGeneratedValue = metaData.hasGeneratedValue();

            try (PreparedStatement stmt = hasGeneratedValue
                    ? connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                    : connection.prepareStatement(sql)) {

                bindInsertParameters(stmt, entity, metaData);
                stmt.executeUpdate();

                if (hasGeneratedValue) {
                    Field idField = metaData.getIdField();
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            Object generatedId = generatedKeys.getObject(1);
                            idField.setAccessible(true);
                            idField.set(entity, generatedId);
                        }
                    }
                }

                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw new RuntimeException("Save operation failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    private <T> void bindInsertParameters(PreparedStatement stmt, T entity, EntityMetaData metaData) throws SQLException, IllegalAccessException {
        List<Field> fields = metaData.getColumnFields();
        int index = 1;

        for (Field field : fields) {
            if (metaData.isGeneratedValue(field)) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(entity);

            if (field.isAnnotationPresent(ManyToOne.class)) {
                if (value == null) {
                    stmt.setObject(index++, null);
                    continue;
                }
                value = getForeignKeyValue(value);
            }

            stmt.setObject(index++, value);
        }
    }

    private Object getForeignKeyValue(Object foreignEntity) {
        try {
            Field idField = foreignEntity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            return idField.get(foreignEntity);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to extract foreign key from related entity", e);
        }
    }

    public <T> T findById(Class<T> clazz, Object id) {
        EntityMetaData metaData = getMetaData(clazz);
        String sql = SQLGenerator.buildSelectById(metaData);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs, clazz, metaData);
                }
                return null;
            }
        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException("FindById operation failed", e);
        }
    }

    private <T> T mapResultSetToEntity(ResultSet rs, Class<T> clazz, EntityMetaData metaData)
            throws ReflectiveOperationException, SQLException {

        T entity = clazz.getDeclaredConstructor().newInstance();

        for (Field field : metaData.getColumnFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
                String fkColumn = getForeignKeyColumnName(field, metaData);
                Object fkValue = rs.getObject(fkColumn);

                if (fkValue != null) {
                    Object refEntity = findById(field.getType(), fkValue);
                    field.set(entity, refEntity);
                }
            } else {
                String columnName = metaData.getColumnName(field);
                Object value = rs.getObject(columnName);
                field.set(entity, value);
            }
        }

        return entity;
    }

    private String getForeignKeyColumnName(Field field, EntityMetaData metaData) {
        if (field.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (!joinColumn.name().isEmpty()) {
                return joinColumn.name().toUpperCase();
            }
        }
        return field.getName().toUpperCase() + "_ID";
    }


    public <T> List<T> findAll(Class<T> clazz) {
        EntityMetaData metaData = getMetaData(clazz);
        String sql = SQLGenerator.buildSelectAllQuery(metaData);
        List<T> resultList = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                resultList.add(mapResultSetToEntity(rs, clazz, metaData));
            }
        } catch (SQLException | ReflectiveOperationException e) {
            throw new RuntimeException("FindAll operation failed", e);
        }
        return resultList;
    }

    public <T> void update(T entity) {
        EntityMetaData metaData = getMetaData(entity.getClass());
        String sql = SQLGenerator.buildUpdateQuery(metaData);

        try (Connection connection = dataSource.getConnection()) {
            TransactionManager transactionManager = new TransactionManager(connection);
            transactionManager.begin();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setUpdateParameters(statement, entity, metaData);
                statement.executeUpdate();
                transactionManager.commit();
            } catch (SQLException | IllegalAccessException e) {
                transactionManager.rollback();
                throw new RuntimeException("Update operation failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    private <T> void setUpdateParameters(PreparedStatement statement, T entity, EntityMetaData metaData) throws SQLException, IllegalAccessException {
        List<Field> fields = metaData.getColumnFields();
        Field idField = metaData.getIdField();

        int paramIndex = 1;
        for (Field field : fields) {
            if (!field.equals(idField)) {
                field.setAccessible(true);
                Object value = field.get(entity);
                statement.setObject(paramIndex++, value);
            }
        }

        idField.setAccessible(true);
        Object idValue = idField.get(entity);
        statement.setObject(paramIndex, idValue);
    }

    public <T> void delete(Class<T> clazz, Object id) {
        EntityMetaData metaData = getMetaData(clazz);
        String sql = SQLGenerator.buildDeleteQuery(metaData);

        try (Connection connection = dataSource.getConnection()) {
            TransactionManager transactionManager = new TransactionManager(connection);
            transactionManager.begin();

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, id);
                statement.executeUpdate();
                transactionManager.commit();
            } catch (SQLException e) {
                transactionManager.rollback();
                throw new RuntimeException("Delete operation failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }
}
