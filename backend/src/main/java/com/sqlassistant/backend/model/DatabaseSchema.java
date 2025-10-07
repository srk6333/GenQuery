package com.sqlassistant.backend.model;

import java.util.List;
import java.util.Map;

public record DatabaseSchema(
    String databaseName,
    List<TableInfo> tables,
    List<ViewInfo> views,
    Map<String, Object> metadata
) {
    
    public record TableInfo(
        String name,
        String schema,
        String type,
        List<ColumnInfo> columns,
        List<IndexInfo> indexes,
        List<ForeignKeyInfo> foreignKeys,
        String comment
    ) {}
    
    public record ColumnInfo(
        String name,
        String dataType,
        String columnType,
        boolean nullable,
        boolean isPrimaryKey,
        boolean isAutoIncrement,
        Object defaultValue,
        String comment,
        Integer maxLength,
        Integer precision,
        Integer scale
    ) {}
    
    public record IndexInfo(
        String name,
        boolean isUnique,
        List<String> columns,
        String type
    ) {}
    
    public record ForeignKeyInfo(
        String name,
        String columnName,
        String referencedTable,
        String referencedColumn,
        String onUpdate,
        String onDelete
    ) {}
    
    public record ViewInfo(
        String name,
        String schema,
        String definition,
        List<ColumnInfo> columns,
        String comment
    ) {}
}