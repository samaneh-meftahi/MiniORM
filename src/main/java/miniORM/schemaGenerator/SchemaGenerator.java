package miniORM.schemaGenerator;

import miniORM.annotation.Column;
import miniORM.annotation.GeneratedValue;
import miniORM.annotation.Id;
import miniORM.annotation.Relation.*;
import miniORM.metaData.EntityMetaData;
import miniORM.db.DataSourceProvider;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

public class SchemaGenerator {

    private static final Set<String> createdJoinTables = new HashSet<>();

    public static List<String> buildCreateTableQuery(Set<Class<?>> classes) {
        List<String> queries = new ArrayList<>();

        for (Class<?> clazz : classes) {
            EntityMetaData metaData = new EntityMetaData(clazz);

            queries.add(buildMainTable(metaData));
            queries.addAll(buildJoinTables(metaData));
        }
        return queries;
    }

    private static String buildMainTable(EntityMetaData metaData) {
        String tableName = metaData.getTableName().toUpperCase();

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner foreignKeys = new StringJoiner(", ");

        for (Field field : metaData.getColumnFields()) {
            String columnName = getColumnName(metaData, field).toUpperCase();
            String sqlType;
            boolean isPrimaryKey = field.isAnnotationPresent(Id.class);
            boolean isGenerated = field.isAnnotationPresent(GeneratedValue.class);

            if (field.isAnnotationPresent(ManyToOne.class)) {
                // Here, the foreign key column name is set correctly without adding _ID twice
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null && !joinColumn.name().isEmpty()) {
                    columnName = joinColumn.name().toUpperCase();
                } else {
                    columnName = columnName + "_ID";
                }
                sqlType = "BIGINT";

                EntityMetaData refMeta = new EntityMetaData(field.getType());
                String refTable = refMeta.getTableName().toUpperCase();
                String refPK = refMeta.getIdColumnName().toUpperCase();

                foreignKeys.add("FOREIGN KEY (" + columnName + ") REFERENCES " + refTable + "(" + refPK + ")");
            } else if (field.isAnnotationPresent(OneToOne.class)) {
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                if (joinColumn != null && !joinColumn.name().isEmpty()) {
                    columnName = joinColumn.name().toUpperCase();
                } else {
                    columnName = columnName + "_ID";
                }
                sqlType = "BIGINT";

                EntityMetaData refMeta = new EntityMetaData(field.getType());
                String refTable = refMeta.getTableName().toUpperCase();
                String refPK = refMeta.getIdColumnName().toUpperCase();

                columns.add(columnName + " " + sqlType + " UNIQUE");
                foreignKeys.add("FOREIGN KEY (" + columnName + ") REFERENCES " + refTable + "(" + refPK + ")");
                continue;
            } else {
                sqlType = mapJavaTypeToSqlType(field.getType());
            }

            String columnDef = columnName + " " + sqlType;

            if (isPrimaryKey) {
                columnDef += " PRIMARY KEY NOT NULL";
                if (isGenerated) {
                    // Add auto_increment for auto-generated primary keys
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

    private static List<String> buildJoinTables(EntityMetaData metaData) {
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

            // Prevent duplicate join table creation
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

    private static String mapJavaTypeToSqlType(Class<?> type) {
        if (type == String.class) return "VARCHAR(255)";
        if (type == int.class || type == Integer.class) return "INTEGER";
        if (type == long.class || type == Long.class) return "BIGINT";
        if (type == boolean.class || type == Boolean.class) return "BOOLEAN";
        if (type == double.class || type == Double.class) return "DOUBLE PRECISION";
        if (type == float.class || type == Float.class) return "FLOAT";
        if (type == short.class || type == Short.class) return "SMALLINT";
        if (type == byte.class || type == Byte.class) return "TINYINT";
        if (type == char.class || type == Character.class) return "CHAR(1)";
        if (type == java.time.LocalDate.class) return "DATE";
        if (type == java.time.LocalDateTime.class) return "TIMESTAMP";
        if (type == java.time.LocalTime.class) return "TIME";
        if (type == java.math.BigDecimal.class) return "DECIMAL(19,2)";
        if (type == java.util.UUID.class) return "UUID";

        throw new IllegalArgumentException("Unsupported Java type for SQL mapping: " + type.getName());
    }

    public static void initializeDatabase() {
        List<String> parentQueries = buildCreateTableQuery(EntityUtil.findEntitiesWithoutRelations());
        List<String> childQueries = buildCreateTableQuery(EntityUtil.findEntitiesWithRelations());
        try (Connection connection = DataSourceProvider.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            for (String query : parentQueries) {
                statement.execute(query);
            }
            System.out.println("Parent tables created automatically!");

            for (String query : childQueries) {
                statement.execute(query);
            }
            System.out.println("Child tables created automatically!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
