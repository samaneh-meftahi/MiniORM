package miniORM.schemaGenerator;

public class SqlTypeMapper {
    public static String mapJavaTypeToSqlType(Class<?> type) {
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
}
