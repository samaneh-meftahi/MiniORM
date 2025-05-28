package miniORM.schemaGenerator;

import miniORM.db.DataSourceProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseSchemaReader {

    public static Map<String, String> getTableColumns(String tableName) {
        Map<String, String> columns = new HashMap<>();
        DataSource dataSource = DataSourceProvider.getDataSource();

        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {

            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME").toUpperCase();
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");

                String columnDef = typeName;
                if (typeName.equalsIgnoreCase("VARCHAR")) {
                    columnDef += "(" + columnSize + ")";
                }

                columns.put(columnName, columnName + " " + columnDef);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error reading table schema: " + tableName, e);
        }

        return columns;
    }
}
