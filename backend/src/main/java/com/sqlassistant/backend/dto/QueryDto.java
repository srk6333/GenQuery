package com.sqlassistant.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class QueryDto {
    
    public record QueryRequest(
        @NotBlank
        String naturalLanguageQuery,
        
        String connectionId,

        DatabaseConnectionDto connectionDto,
        
        Map<String, Object> context
    ) {}
    
    public record QueryResponse(
        String generatedSql,
        String explanation,
        List<String> warnings,
        boolean isExecutable,
        QueryMetadata metadata
    ) {}
    
    public record QueryExecutionRequest(
        @NotBlank
        String sql,
        
        String connectionId,
        
        Integer limit,
        DatabaseConnectionDto connectionDto,
        
        Integer offset,
        
        boolean dryRun
    ) {}
    
    public record QueryExecutionResponse(
        List<Map<String, Object>> results,
        List<String> columnNames,
        List<String> columnTypes,
        int rowCount,
        long executionTimeMs,
        String status,
        String error,
        QueryMetadata metadata
    ) {}
    
    public record QueryMetadata(
        String queryType,
        List<String> tablesInvolved,
        boolean hasJoins,
        boolean hasSubqueries,
        boolean hasAggregations,
        String complexity,
        LocalDateTime timestamp
    ) {}
    
    public record QueryValidationResponse(
        boolean isValid,
        List<String> errors,
        List<String> warnings,
        List<String> suggestions,
        String sanitizedQuery
    ) {}
}