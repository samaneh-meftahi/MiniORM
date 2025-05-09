package miniORM.sql;

import miniORM.annotation.Relation.JoinColumn;
import miniORM.annotation.Relation.ManyToOne;
import miniORM.metaData.EntityMetaData;

import java.lang.reflect.Field;
import java.util.StringJoiner;

public class SQLGenerator {

    public static String buildInsertQuery(EntityMetaData metaData) {
        StringJoiner columns = new StringJoiner(",");
        StringJoiner placeholders = new StringJoiner(",");

        for (Field field : metaData.getColumnFields()) {
            if (!metaData.isGeneratedValue(field)) {
                String columnName = getColumnNameForField(field, metaData);
                columns.add(columnName);
                placeholders.add("?");
            }
        }

        return "INSERT INTO " + metaData.getTableName() + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    public static String buildSelectById(EntityMetaData metaData) {
        return "SELECT * FROM " + metaData.getTableName() + " WHERE " + metaData.getIdColumnName() + " = ?";
    }

    public static String buildSelectAllQuery(EntityMetaData metaData) {
        return "SELECT * FROM " + metaData.getTableName();
    }

    public static String buildUpdateQuery(EntityMetaData metaData) {
        StringJoiner assignments = new StringJoiner(",");
        Field idField = metaData.getIdField();

        for (Field field : metaData.getColumnFields()) {
            if (!field.equals(idField)) {
                String columnName = getColumnNameForField(field, metaData);
                assignments.add(columnName + " = ?");
            }
        }

        return "UPDATE " + metaData.getTableName() + " SET " + assignments + " WHERE " + metaData.getIdColumnName() + " = ?";
    }

    public static String buildDeleteQuery(EntityMetaData metaData) {
        return "DELETE FROM " + metaData.getTableName() + " WHERE " + metaData.getIdColumnName() + " = ?";
    }

    private static String getColumnNameForField(Field field, EntityMetaData metaData) {
        if (field.isAnnotationPresent(ManyToOne.class)) {
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null && !joinColumn.name().isEmpty()) {
                return joinColumn.name();
            } else {
                return field.getName() + "_id";
            }
        } else {
            return metaData.getColumnName(field);
        }
    }
}
