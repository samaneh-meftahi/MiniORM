package miniORM.schemaGenerator;

import miniORM.exception.OrmDatabaseException;
import miniORM.metaData.EntityMetaData;
import miniORM.db.DataSourceProvider;
import miniORM.schemaGenerator.config.SchemaGenerationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

public class SchemaGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SchemaGenerator.class);

    /**
     * Generates CREATE TABLE queries for a set of entity classes.
     */
    public static List<String> buildCreateTableQueries(Set<Class<?>> entityClasses) {
        return CreateQueryGenerator.generateCreateQueries(entityClasses);
    }

    /**
     * Main entry point to initialize or update database schema.
     */
    public static void initializeDatabase(SchemaGenerationStrategy strategy) {
        try (Connection connection = DataSourceProvider.getDataSource().getConnection();
             Statement statement = connection.createStatement()) {

            switch (strategy) {
                case CREATE:
                    DropQueryGenerator.dropAllTables(statement);

                    Set<Class<?>> parents = EntityUtil.findEntitiesWithoutRelations();
                    List<String> parentQueries = buildCreateTableQueries(parents);
                    executeQueries(statement, parentQueries, "Parent tables created.");

                    Set<Class<?>> children = EntityUtil.findEntitiesWithRelations();
                    List<String> childQueries = buildCreateTableQueries(children);
                    executeQueries(statement, childQueries, "Child and join tables created.");
                    break;

                case UPDATE:
                    UpdateQueryGenerator.updateSchema(statement);
                    break;

                case NONE:
                    logger.info("No schema generation performed (strategy=NONE).");
                    break;

                default:
                    throw new IllegalArgumentException("Unknown schema generation strategy: " + strategy);
            }
        } catch (Exception e) {
            logger.error("Schema generation failed.", e);
            throw new OrmDatabaseException("Schema generation failed.", e);
        }
    }

    /**
     * Utility to execute a list of queries and log a message.
     */
    private static void executeQueries(Statement statement, List<String> queries, String successMessage) throws Exception {
        for (String q : queries) {
            statement.execute(q);
        }
        logger.info(successMessage);
    }
}
