package miniORM.schemaGenerator;

import miniORM.annotation.*;
import miniORM.annotation.Relation.*;
import miniORM.metaData.EntityMetaData;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class CreateQueryGenerator {

    private static final Set<String> createdJoinTables = new HashSet<>();

    public static List<String> generateCreateQueries(Set<Class<?>> entityClasses) {
        List<String> queries = new ArrayList<>();

        for (Class<?> clazz : entityClasses) {
            EntityMetaData metaData = new EntityMetaData(clazz);
            queries.add(buildMainTable(metaData));
            queries.addAll(buildJoinTables(metaData));
        }
        return queries;
    }

     static String buildMainTable(EntityMetaData metaData) {
        String tableName = metaData.getTableName().toUpperCase();

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner foreignKeys = new StringJoiner(", ");

        for (Field field : metaData.getColumnFields()) {
            String columnName = getColumnName(metaData, field).toUpperCase();
            String sqlType;
            boolean isPrimaryKey = field.isAnnotationPresent(Id.class);
            boolean isGenerated = field.isAnnotationPresent(GeneratedValue.class);

            if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null && !joinColumn.name().isEmpty()) {
                    columnName = joinColumn.name().toUpperCase();
                } else {
                    columnName = columnName + "_ID";
                }

                EntityMetaData refMeta = new EntityMetaData(field.getType());
                sqlType = SqlTypeMapper.mapJavaTypeToSqlType(refMeta.getIdField().getType());

                if (field.isAnnotationPresent(OneToOne.class)) {
                    columns.add(columnName + " " + sqlType + " UNIQUE");
                } else {
                    columns.add(columnName + " " + sqlType);
                }

                String refTable = refMeta.getTableName().toUpperCase();
                String refPK = refMeta.getIdColumnName().toUpperCase();
                foreignKeys.add("FOREIGN KEY (" + columnName + ") REFERENCES " + refTable + "(" + refPK + ")");

                continue;
            } else {
                sqlType = SqlTypeMapper.mapJavaTypeToSqlType(field.getType());
            }

            String columnDef = columnName + " " + sqlType;

            if (isPrimaryKey) {
                columnDef += " PRIMARY KEY NOT NULL";
                if (isGenerated) {
                    columnDef += " AUTO_INCREMENT";
                }
            }

            columns.add(columnDef);
        }

        String fullColumns = columns.toString();

        if (foreignKeys.length() > 0) {
            fullColumns += ", " + foreignKeys.toString();
        }

        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" + fullColumns + ")";
    }

     static List<String> buildJoinTables(EntityMetaData metaData) {
        List<String> joinQueries = new ArrayList<>();

        for (Field field : metaData.getAllRelationFields()) {
            if (!field.isAnnotationPresent(ManyToMany.class)) continue;

            Type genericType = field.getGenericType();
            if (!(genericType instanceof ParameterizedType)) continue;

            ParameterizedType pt = (ParameterizedType) genericType;
            Class<?> genericClass = (Class<?>) pt.getActualTypeArguments()[0];

            EntityMetaData targetMeta = new EntityMetaData(genericClass);

            List<String> tables = Arrays.asList(metaData.getTableName().toUpperCase(), targetMeta.getTableName().toUpperCase());
            Collections.sort(tables);
            String joinTableName = tables.get(0) + "_" + tables.get(1);

            if (createdJoinTables.contains(joinTableName)) {
                continue;
            }
            createdJoinTables.add(joinTableName);

            String pkThis = metaData.getIdColumnName().toUpperCase();
            String pkOther = targetMeta.getIdColumnName().toUpperCase();

            String columnThis = tables.get(0) + "_" + pkThis;
            String columnOther = tables.get(1) + "_" + pkOther;

            String joinTableQuery = "CREATE TABLE IF NOT EXISTS " + joinTableName + " (" +
                    columnThis + " BIGINT NOT NULL, " +
                    columnOther + " BIGINT NOT NULL, " +
                    "PRIMARY KEY (" + columnThis + ", " + columnOther + "), " +
                    "FOREIGN KEY (" + columnThis + ") REFERENCES " + tables.get(0) + "(" + pkThis + "), " +
                    "FOREIGN KEY (" + columnOther + ") REFERENCES " + tables.get(1) + "(" + pkOther + ")" +
                    ")";

            joinQueries.add(joinTableQuery);
        }

        return joinQueries;
    }

    private static String getColumnName(EntityMetaData metaData, Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            String name = field.getAnnotation(Column.class).name();
            if (!name.isEmpty()) return name;
        }
        return field.getName();
    }
    public static Set<String> getCreatedJoinTables() {
        return createdJoinTables;
    }

}
