package com.sqlassistant.backend.controller;

import com.sqlassistant.backend.dto.DatabaseConnectionDto;
import com.sqlassistant.backend.dto.QueryDto;
import com.sqlassistant.backend.model.DatabaseSchema;
import com.sqlassistant.backend.service.AiService;
import com.sqlassistant.backend.service.DatabaseService;
import com.sqlassistant.backend.service.QueryValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/query")
public class QueryController {
    
    private final AiService aiService;
    private final DatabaseService databaseService;
    private final QueryValidationService validationService;
    
    public QueryController(AiService aiService, DatabaseService databaseService, 
                          QueryValidationService validationService) {
        this.aiService = aiService;
        this.databaseService = databaseService;
        this.validationService = validationService;
    }
    
    @PostMapping("/generate")
    public Mono<ResponseEntity<Map<String, Object>>> generateQuery(
            @Valid @RequestBody QueryDto.QueryRequest request,
            @RequestParam String connectionId
            // @RequestBody DatabaseConnectionDto connectionDto
    ) {
        
        try {
            // Get database schema first
            DatabaseSchema schema = databaseService.getSchema(connectionId, request.connectionDto());
            
            // Generate SQL using AI
            return Mono.just(ResponseEntity.ok(Map.of(
                    "query", aiService.generateSqlQuery(
                            request.naturalLanguageQuery(),
                            schema,
                            request.context()
                    ),
                    "status", "SUCCESS"
            )));

        } catch (SQLException e) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "Database connection error: " + e.getMessage()
            )));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "Unexpected error: " + e.getMessage()
            )));
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<?> validateQuery(@Valid @RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "SQL query is required"
            ));
        }
        
        QueryDto.QueryValidationResponse validation = validationService.validateQuery(sql);
        
        return ResponseEntity.ok(Map.of(
            "validation", validation,
            "status", "SUCCESS"
        ));
    }
    
    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(
            @Valid @RequestBody QueryDto.QueryExecutionRequest request,
            @RequestParam String connectionId
            //@RequestBody DatabaseConnectionDto connectionDto
    ) {
        
        try {
            QueryDto.QueryExecutionResponse response = databaseService.executeQuery(
                connectionId, request.connectionDto(), request
            );
            
            return ResponseEntity.ok(Map.of(
                "execution", response,
                "status", "SUCCESS"
            ));
            
        } catch (SQLException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "Query execution failed: " + e.getMessage(),
                "error", e.getSQLState()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "Unexpected error: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/explain")
    public Mono<ResponseEntity<Map<String, String>>> explainQuery(
            @RequestBody Map<String, String> request,
            @RequestParam String connectionId,
            @RequestBody DatabaseConnectionDto connectionDto) {
        
        String sql = request.get("sql");
        
        if (sql == null || sql.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "SQL query is required"
            )));
        }
        
        try {
            // Get database schema for context
            DatabaseSchema schema = databaseService.getSchema(connectionId, connectionDto);
            
            return aiService.explainQuery(sql, schema)
                .map(explanation -> ResponseEntity.ok(Map.of(
                    "explanation", explanation,
                    "status", "SUCCESS"
                )))
                .onErrorResume(error -> Mono.just(ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to generate explanation"
                ))));
                
        } catch (SQLException e) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "Database connection error: " + e.getMessage()
            )));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "Unexpected error: " + e.getMessage()
            )));
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<?> getQueryHistory(@RequestParam String connectionId) {
        // This would be implemented with a proper query history storage system
        // For now, return empty history
        return ResponseEntity.ok(Map.of(
            "history", java.util.Collections.emptyList(),
            "status", "SUCCESS",
            "message", "Query history feature not yet implemented"
        ));
    }
    
    @PostMapping("/save")
    public ResponseEntity<?> saveQuery(@RequestBody Map<String, Object> request) {
        // This would be implemented with a proper query storage system
        // For now, just acknowledge the request
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Query save feature not yet implemented"
        ));
    }
}