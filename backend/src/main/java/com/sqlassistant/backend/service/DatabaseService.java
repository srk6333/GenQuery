package com.sqlassistant.backend.service;

import com.sqlassistant.backend.dto.DatabaseConnectionDto;
import com.sqlassistant.backend.dto.QueryDto;
import com.sqlassistant.backend.model.DatabaseSchema;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DatabaseService {
    
    private final Map<String, DataSource> activeConnections = new ConcurrentHashMap<>();
    private final QueryValidationService validationService;
    
    public DatabaseService(QueryValidationService validationService) {
        this.validationService = validationService;
    }
    
    public String testConnection(DatabaseConnectionDto connectionDto) throws SQLException {
        String connectionId = generateConnectionId(connectionDto);
        
        try (Connection connection = createConnection(connectionDto)) {
            if (connection.isValid(5)) {
                return connectionId;
            }
        }
        
        throw new SQLException("Connection test failed");
    }
    
    public DatabaseSchema getSchema(String connectionId, DatabaseConnectionDto connectionDto) throws SQLException {
        try (Connection connection = createConnection(connectionDto)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            List<DatabaseSchema.TableInfo> tables = new ArrayList<>();
            List<DatabaseSchema.ViewInfo> views = new ArrayList<>();
            
            // Get tables
            try (ResultSet tablesResultSet = metaData.getTables(
                connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                
                while (tablesResultSet.next()) {
                    String tableName = tablesResultSet.getString("TABLE_NAME");
                    String schema = tablesResultSet.getString("TABLE_SCHEM");
                    String tableType = tablesResultSet.getString("TABLE_TYPE");
                    String comment = tablesResultSet.getString("REMARKS");
                    
                    List<DatabaseSchema.ColumnInfo> columns = getTableColumns(metaData, schema, tableName);
                    List<DatabaseSchema.IndexInfo> indexes = getTableIndexes(metaData, schema, tableName);
                    List<DatabaseSchema.ForeignKeyInfo> foreignKeys = getTableForeignKeys(metaData, schema, tableName);
                    
                    tables.add(new DatabaseSchema.TableInfo(
                        tableName, schema, tableType, columns, indexes, foreignKeys, comment
                    ));
                }
            }
            
            // Get views
            try (ResultSet viewsResultSet = metaData.getTables(
                connection.getCatalog(), null, "%", new String[]{"VIEW"})) {
                
                while (viewsResultSet.next()) {
                    String viewName = viewsResultSet.getString("TABLE_NAME");
                    String schema = viewsResultSet.getString("TABLE_SCHEM");
                    String comment = viewsResultSet.getString("REMARKS");
                    
                    List<DatabaseSchema.ColumnInfo> columns = getTableColumns(metaData, schema, viewName);
                    
                    views.add(new DatabaseSchema.ViewInfo(
                        viewName, schema, null, columns, comment
                    ));
                }
            }
            
            Map<String, Object> metadata = Map.of(
                "databaseProductName", metaData.getDatabaseProductName(),
                "databaseProductVersion", metaData.getDatabaseProductVersion(),
                "driverName", metaData.getDriverName(),
                "driverVersion", metaData.getDriverVersion(),
                "catalogTerm", metaData.getCatalogTerm(),
                "schemaTerm", metaData.getSchemaTerm()
            );
            
            return new DatabaseSchema(connection.getCatalog(), tables, views, metadata);
        }
    }
    
    public QueryDto.QueryExecutionResponse executeQuery(
        String connectionId, 
        DatabaseConnectionDto connectionDto, 
        QueryDto.QueryExecutionRequest request) throws SQLException {
        
        // Validate query first
        QueryDto.QueryValidationResponse validation = validationService.validateQuery(request.sql());
        if (!validation.isValid()) {
            return new QueryDto.QueryExecutionResponse(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                0,
                "VALIDATION_FAILED",
                String.join("; ", validation.errors()),
                null
            );
        }
        
        if (request.dryRun()) {
            return new QueryDto.QueryExecutionResponse(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                0,
                "DRY_RUN_SUCCESS",
                null,
                createQueryMetadata(request.sql())
            );
        }
        
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = createConnection(connectionDto);
             Statement statement = connection.createStatement()) {
            
            // Set query timeout and limits
            statement.setQueryTimeout(30);
            if (request.limit() != null) {
                statement.setMaxRows(request.limit());
            }
            
            try (ResultSet resultSet = statement.executeQuery(request.sql())) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                List<String> columnNames = new ArrayList<>();
                List<String> columnTypes = new ArrayList<>();
                
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                    columnTypes.add(metaData.getColumnTypeName(i));
                }
                
                List<Map<String, Object>> results = new ArrayList<>();
                int rowCount = 0;
                
                while (resultSet.next() && rowCount < 1000) { // Max 1000 rows
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(columnNames.get(i - 1), resultSet.getObject(i));
                    }
                    results.add(row);
                    rowCount++;
                }
                
                long executionTime = System.currentTimeMillis() - startTime;
                
                return new QueryDto.QueryExecutionResponse(
                    results,
                    columnNames,
                    columnTypes,
                    rowCount,
                    executionTime,
                    "SUCCESS",
                    null,
                    createQueryMetadata(request.sql())
                );
            }
            
        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            return new QueryDto.QueryExecutionResponse(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                executionTime,
                "ERROR",
                e.getMessage(),
                createQueryMetadata(request.sql())
            );
        }
    }
    
    private Connection createConnection(DatabaseConnectionDto connectionDto) throws SQLException {
        String url = connectionDto.buildConnectionUrl();
        String username = connectionDto.username() != null ? connectionDto.username() : "";
        String password = connectionDto.password() != null ? connectionDto.password() : "";
        
        return DriverManager.getConnection(url, username, password);
    }
    
    private String generateConnectionId(DatabaseConnectionDto connectionDto) {
        return String.valueOf(
            Objects.hash(connectionDto.type(), connectionDto.host(), 
                        connectionDto.port(), connectionDto.database())
        );
    }
    
    private List<DatabaseSchema.ColumnInfo> getTableColumns(
        DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        
        List<DatabaseSchema.ColumnInfo> columns = new ArrayList<>();
        Set<String> primaryKeys = getPrimaryKeys(metaData, schema, tableName);
        
        try (ResultSet columnsResultSet = metaData.getColumns(null, schema, tableName, "%")) {
            while (columnsResultSet.next()) {
                String columnName = columnsResultSet.getString("COLUMN_NAME");
                String dataType = columnsResultSet.getString("DATA_TYPE");
                String columnType = columnsResultSet.getString("TYPE_NAME");
                boolean nullable = "YES".equals(columnsResultSet.getString("IS_NULLABLE"));
                String defaultValue = columnsResultSet.getString("COLUMN_DEF");
                String comment = columnsResultSet.getString("REMARKS");
                Integer columnSize = columnsResultSet.getInt("COLUMN_SIZE");
                Integer decimalDigits = columnsResultSet.getInt("DECIMAL_DIGITS");
                String isAutoIncrement = columnsResultSet.getString("IS_AUTOINCREMENT");
                
                columns.add(new DatabaseSchema.ColumnInfo(
                    columnName,
                    dataType,
                    columnType,
                    nullable,
                    primaryKeys.contains(columnName),
                    "YES".equals(isAutoIncrement),
                    defaultValue,
                    comment,
                    columnSize,
                    columnSize,
                    decimalDigits
                ));
            }
        }
        
        return columns;
    }
    
    private Set<String> getPrimaryKeys(DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        Set<String> primaryKeys = new HashSet<>();
        
        try (ResultSet pkResultSet = metaData.getPrimaryKeys(null, schema, tableName)) {
            while (pkResultSet.next()) {
                primaryKeys.add(pkResultSet.getString("COLUMN_NAME"));
            }
        }
        
        return primaryKeys;
    }
    
    private List<DatabaseSchema.IndexInfo> getTableIndexes(
        DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        
        List<DatabaseSchema.IndexInfo> indexes = new ArrayList<>();
        Map<String, List<String>> indexColumns = new HashMap<>();
        Map<String, Boolean> indexUnique = new HashMap<>();
        Map<String, String> indexType = new HashMap<>();
        
        try (ResultSet indexResultSet = metaData.getIndexInfo(null, schema, tableName, false, false)) {
            while (indexResultSet.next()) {
                String indexName = indexResultSet.getString("INDEX_NAME");
                if (indexName == null) continue;
                
                String columnName = indexResultSet.getString("COLUMN_NAME");
                boolean nonUnique = indexResultSet.getBoolean("NON_UNIQUE");
                String type = String.valueOf(indexResultSet.getShort("TYPE"));
                
                indexColumns.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName);
                indexUnique.put(indexName, !nonUnique);
                indexType.put(indexName, type);
            }
        }
        
        for (Map.Entry<String, List<String>> entry : indexColumns.entrySet()) {
            indexes.add(new DatabaseSchema.IndexInfo(
                entry.getKey(),
                indexUnique.get(entry.getKey()),
                entry.getValue(),
                indexType.get(entry.getKey())
            ));
        }
        
        return indexes;
    }
    
    private List<DatabaseSchema.ForeignKeyInfo> getTableForeignKeys(
        DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
        
        List<DatabaseSchema.ForeignKeyInfo> foreignKeys = new ArrayList<>();
        
        try (ResultSet fkResultSet = metaData.getImportedKeys(null, schema, tableName)) {
            while (fkResultSet.next()) {
                String fkName = fkResultSet.getString("FK_NAME");
                String columnName = fkResultSet.getString("FKCOLUMN_NAME");
                String referencedTable = fkResultSet.getString("PKTABLE_NAME");
                String referencedColumn = fkResultSet.getString("PKCOLUMN_NAME");
                String onUpdate = getKeyAction(fkResultSet.getShort("UPDATE_RULE"));
                String onDelete = getKeyAction(fkResultSet.getShort("DELETE_RULE"));
                
                foreignKeys.add(new DatabaseSchema.ForeignKeyInfo(
                    fkName, columnName, referencedTable, referencedColumn, onUpdate, onDelete
                ));
            }
        }
        
        return foreignKeys;
    }
    
    private String getKeyAction(short action) {
        return switch (action) {
            case DatabaseMetaData.importedKeyCascade -> "CASCADE";
            case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
            case DatabaseMetaData.importedKeySetNull -> "SET NULL";
            case DatabaseMetaData.importedKeySetDefault -> "SET DEFAULT";
            default -> "NO ACTION";
        };
    }
    
    private QueryDto.QueryMetadata createQueryMetadata(String sql) {
        // Simple query analysis - in a real implementation, you'd use a proper SQL parser
        String upperSql = sql.toUpperCase().trim();
        
        String queryType = "SELECT";
        if (upperSql.startsWith("INSERT")) queryType = "INSERT";
        else if (upperSql.startsWith("UPDATE")) queryType = "UPDATE";
        else if (upperSql.startsWith("DELETE")) queryType = "DELETE";
        else if (upperSql.startsWith("CREATE")) queryType = "CREATE";
        else if (upperSql.startsWith("DROP")) queryType = "DROP";
        else if (upperSql.startsWith("ALTER")) queryType = "ALTER";
        
        boolean hasJoins = upperSql.contains(" JOIN ");
        boolean hasSubqueries = upperSql.contains("(SELECT") || upperSql.contains("( SELECT");
        boolean hasAggregations = upperSql.contains("COUNT(") || upperSql.contains("SUM(") || 
                                 upperSql.contains("AVG(") || upperSql.contains("MIN(") || 
                                 upperSql.contains("MAX(") || upperSql.contains("GROUP BY");
        
        String complexity = "SIMPLE";
        if (hasSubqueries || (hasJoins && hasAggregations)) complexity = "COMPLEX";
        else if (hasJoins || hasAggregations) complexity = "MODERATE";
        
        return new QueryDto.QueryMetadata(
            queryType,
            Collections.emptyList(), // Would extract table names in real implementation
            hasJoins,
            hasSubqueries,
            hasAggregations,
            complexity,
            LocalDateTime.now()
        );
    }
}