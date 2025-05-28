package miniORM.metaData;

import miniORM.annotation.Column;
import miniORM.annotation.Entity;
import miniORM.annotation.GeneratedValue;
import miniORM.annotation.Id;
import miniORM.annotation.Relation.*;
import miniORM.exception.OrmException;
import miniORM.schemaGenerator.SqlTypeMapper;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Extracts metadata from entity classes for ORM mapping.
 */
public class EntityMetaData {
    private final Class<?> clazz;

    public EntityMetaData(Class<?> clazz) {
        this.clazz = clazz;
        validateEntityClass(clazz);
    }

    private void validateEntityClass(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new OrmException("Class " + clazz.getName() + " is not annotated with @Entity");
        }
    }

    public String getTableName() {
        Entity entityAnnotation = clazz.getAnnotation(Entity.class);
        String tableName = entityAnnotation.tableName();
        if (tableName == null || tableName.trim().isEmpty()) {
            return clazz.getSimpleName().toUpperCase();
        }
        return tableName.toUpperCase();
    }

    public boolean isGeneratedValue(Field field) {
        return field.isAnnotationPresent(GeneratedValue.class);
    }

    public boolean hasGeneratedValue() {
        for (Field field : getColumnFields()) {
            if (isGeneratedValue(field)) {
                return true;
            }
        }
        return false;
    }

    public List<Field> getAllRelationFields() {
        List<Field> relationFields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(OneToOne.class)
                    || field.isAnnotationPresent(OneToMany.class)
                    || field.isAnnotationPresent(ManyToOne.class)
                    || field.isAnnotationPresent(ManyToMany.class)) {
                relationFields.add(field);
            }
        }
        return relationFields;
    }

    public String getJoinColumnName(Field field) {
        if (field.isAnnotationPresent(JoinColumn.class)) {
            String name = field.getAnnotation(JoinColumn.class).name();
            if (name != null && !name.trim().isEmpty()) {
                return name.toUpperCase();
            }
        }
        return field.getName().toUpperCase() + "_ID";
    }

    public Field getIdField() {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new RuntimeException("No field annotated with @Id in " + clazz.getSimpleName());
    }

    public String getIdColumnName() {
        Field idField = getIdField();
        if (idField.isAnnotationPresent(Column.class)) {
            String name = idField.getAnnotation(Column.class).name();
            if (name != null && !name.trim().isEmpty()) {
                return name.toUpperCase();
            }
        }
        return idField.getName().toUpperCase();
    }

    public List<Field> getColumnFields() {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)
                    || field.isAnnotationPresent(Id.class)
                    || field.isAnnotationPresent(ManyToOne.class)
                    || field.isAnnotationPresent(OneToOne.class)) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }

    public String getColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null && columnAnnotation.name() != null && !columnAnnotation.name().trim().isEmpty()) {
            return columnAnnotation.name().toUpperCase();
        }
        return field.getName().toUpperCase();
    }

    public Object getFieldValue(Object entity, Field field) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field " + field.getName() + " in " + entity.getClass().getSimpleName(), e);
        }
    }

    public Map<String, String> getColumnDefinitions() {
        Map<String, String> columnDefinitions = new LinkedHashMap<>();

        for (Field field : getColumnFields()) {
            // Skip collection relations as they do not have columns in this table
            if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                continue;
            }

            String columnName;
            String columnType;

            if (isEntityType(field.getType()) && (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class))) {
                // For relation fields, define FK column
                columnName = getJoinColumnName(field);

                // Determine FK column type based on referenced entity's ID type
                Class<?> referencedEntity = field.getType();
                EntityMetaData referencedMeta = new EntityMetaData(referencedEntity);
                Field referencedIdField = referencedMeta.getIdField();
                columnType = SqlTypeMapper.mapJavaTypeToSqlType(referencedIdField.getType());
            } else {
                columnName = getColumnName(field);
                columnType = SqlTypeMapper.mapJavaTypeToSqlType(field.getType());
            }

            StringBuilder definition = new StringBuilder(columnName + " " + columnType);

            if (field.isAnnotationPresent(Id.class)) {
                definition.append(" PRIMARY KEY");
                if (isGeneratedValue(field)) {
                    definition.append(" AUTO_INCREMENT");
                }
            }

            // Additional constraints (e.g., NOT NULL) can be added here if needed

            columnDefinitions.put(columnName, definition.toString());
        }

        return columnDefinitions;
    }

    private boolean isEntityType(Class<?> type) {
        return type.isAnnotationPresent(Entity.class);
    }
}

