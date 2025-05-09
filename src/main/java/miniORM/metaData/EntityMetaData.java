package miniORM.metaData;

import miniORM.annotation.Column;
import miniORM.annotation.Entity;
import miniORM.annotation.GeneratedValue;
import miniORM.annotation.Id;
import miniORM.annotation.Relation.*;
import miniORM.exception.OrmException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class EntityMetaData {
    private final Class<?> clazz;

    public EntityMetaData(Class<?> clazz) {
        this.clazz = clazz;
    }

    public void validateEntityClass(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new OrmException();
        }
    }

    public String getTableName() {
        validateEntityClass(clazz);
        Entity annotationEntity = clazz.getAnnotation(Entity.class);
        return annotationEntity.tableName().isEmpty() ?
                clazz.getSimpleName() : annotationEntity.tableName();
    }

    public boolean isGeneratedValue(Field field) {
        return field.isAnnotationPresent(GeneratedValue.class);
    }

    public boolean hasGeneratedValue() {
        for (Field field : getColumnFields()) {
            if (field.isAnnotationPresent(GeneratedValue.class)) {
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
            return field.getAnnotation(JoinColumn.class).name();
        }
        throw new RuntimeException("No @JoinColumn found on field: " + field.getName());
    }


    public Field getIdField() {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new RuntimeException("No field annotation whit @Id in " + clazz.getSimpleName());
    }

    public String getIdColumnName() {
        Field idField = getIdField();
        if (idField.isAnnotationPresent(Column.class)) {
            String name = idField.getAnnotation(Column.class).name();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return idField.getName();
    }


    public List<Field> getColumnFields() {
        List<Field> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class) ||
                    field.isAnnotationPresent(Id.class) ||
                    field.isAnnotationPresent(ManyToOne.class)||
                    field.isAnnotationPresent(OneToOne.class)) {
                fields.add(field);
            }
        }
        return fields;
    }


    public String getColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            return columnAnnotation.name();
        }
        return field.getName();
    }


    public Object getFieldValue(Object entity, Field field) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("cannot access field " + field.getName());
        }
    }
}
