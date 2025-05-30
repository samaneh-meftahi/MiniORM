package miniORM.schemaGenerator;

import miniORM.exception.OrmDatabaseException;
import miniORM.exception.OrmException;
import miniORM.metaData.EntityMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.Set;

/**
 * Drops all tables in the database, including join tables.
 */
public class DropQueryGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DropQueryGenerator.class);

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
                EntityMetaData metaData;
                try {
                    metaData = new EntityMetaData(clazz);
                } catch (OrmException e) {
                    logger.warn("Skipping class {} as it is not a valid entity: {}", clazz.getName(), e.getMessage());
                    continue;
                }
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
            throw new OrmDatabaseException("Failed to drop all tables.", e);
        }
    }
}
