package miniORM.schemaGenerator;

import miniORM.metaData.EntityMetaData;
import miniORM.db.DataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Drops all tables in the database, including join tables.
 */
public class DropQueryGenerator {

    final Set<String> createdJoinTables;
    private static final Logger logger = LoggerFactory.getLogger(SchemaGenerator.class);


    private DropQueryGenerator(Set<String> createdJoinTables) {
        this.createdJoinTables = createdJoinTables;
    }


    public static void dropAllTables(Statement statement) {
        try {
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            // Drop join tables first
            Set<String> joinTables = CreateQueryGenerator.getCreatedJoinTables();
            for (String joinTable : joinTables) {
                try {
                    statement.execute("DROP TABLE IF EXISTS " + joinTable);
                    logger.info("Dropped join table: {}", joinTable);
                } catch (Exception e) {
                    logger.warn("Failed to drop join table {}: {}", joinTable, e.getMessage());
                }
            }
            joinTables.clear();

            // Drop entity tables
            Set<Class<?>> allEntities = EntityUtil.getAllEntities();
            for (Class<?> clazz : allEntities) {
                EntityMetaData metaData = new EntityMetaData(clazz);
                String tableName = metaData.getTableName().toUpperCase();
                try {
                    statement.execute("DROP TABLE IF EXISTS " + tableName);
                    logger.info("Dropped table: {}", tableName);
                } catch (Exception e) {
                    logger.warn("Failed to drop table {}: {}", tableName, e.getMessage());
                }
            }

            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
            logger.info("All tables dropped successfully!");
        } catch (Exception e) {
            logger.error("Error while dropping tables.", e);
            throw new RuntimeException("Failed to drop all tables.", e);
        }
    }

}
