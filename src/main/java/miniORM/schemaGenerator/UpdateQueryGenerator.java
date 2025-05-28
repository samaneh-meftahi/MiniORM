package miniORM.schemaGenerator;

import miniORM.metaData.EntityMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Updates the schema by adding new columns (no destructive changes).
 */
public class UpdateQueryGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SchemaGenerator.class);

    static void updateSchema(Statement statement) {


        logger.info("Running UPDATE strategy...");
        Set<Class<?>> allEntities = EntityUtil.getAllEntities();

        for (Class<?> clazz : allEntities) {
            EntityMetaData metaData = new EntityMetaData(clazz);
            String tableName = metaData.getTableName().toUpperCase();

            // Read existing columns from DB
            Map<String, String> dbColumns = DatabaseSchemaReader.getTableColumns(tableName);
            // Columns defined in the entity
            Map<String, String> entityColumns = metaData.getColumnDefinitions();

            // Add new columns
            for (String column : entityColumns.keySet()) {
                if (!dbColumns.containsKey(column)) {
                    String alterQuery = "ALTER TABLE " + tableName + " ADD COLUMN " + entityColumns.get(column) + ";";
                    logger.info("Detected new column '{}' in table '{}'. Running: {}", column, tableName, alterQuery);
                    try {
                        statement.executeUpdate(alterQuery);
                    } catch (Exception e) {
                        logger.warn("Failed to add column {}: {}", column, e.getMessage());
                    }
                }
            }

            // Warn about columns in DB but not in entity (no action taken)
            for (String column : dbColumns.keySet()) {
                if (!entityColumns.containsKey(column)) {
                    logger.info("Column '{}' exists in DB but not in entity '{}'. (No action taken)", column, tableName);
                }
            }
        }
    }
}
