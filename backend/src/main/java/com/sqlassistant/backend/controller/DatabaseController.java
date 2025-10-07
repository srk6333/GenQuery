package com.sqlassistant.backend.controller;

import com.sqlassistant.backend.dto.DatabaseConnectionDto;
import com.sqlassistant.backend.model.DatabaseSchema;
import com.sqlassistant.backend.service.DatabaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/database")
public class DatabaseController {
    
    private final DatabaseService databaseService;
    
    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
    
    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection(@Valid @RequestBody DatabaseConnectionDto connectionDto) {
        try {
            String connectionId = databaseService.testConnection(connectionDto);
            return ResponseEntity.ok(Map.of(
                "connectionId", connectionId,
                "status", "SUCCESS",
                "message", "Connection established successfully"
            ));
        } catch (SQLException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "Connection failed: " + e.getMessage(),
                "error", e.getSQLState()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "Unexpected error: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/schema")
    public ResponseEntity<?> getSchema(
            @Valid @RequestBody DatabaseConnectionDto connectionDto,
            @RequestParam(required = false) String connectionId) {
        
        try {
            if (connectionId == null) {
                connectionId = databaseService.testConnection(connectionDto);
            }
            
            DatabaseSchema schema = databaseService.getSchema(connectionId, connectionDto);
            
            return ResponseEntity.ok(Map.of(
                "connectionId", connectionId,
                "schema", schema,
                "status", "SUCCESS"
            ));
            
        } catch (SQLException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "Failed to retrieve schema: " + e.getMessage(),
                "error", e.getSQLState()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR", 
                "message", "Unexpected error: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/supported-types")
    public ResponseEntity<?> getSupportedDatabaseTypes() {
        return ResponseEntity.ok(Map.of(
            "supportedTypes", DatabaseConnectionDto.DatabaseType.values(),
            "status", "SUCCESS"
        ));
    }
    
    @PostMapping("/validate-connection")
    public ResponseEntity<?> validateConnection(@Valid @RequestBody DatabaseConnectionDto connectionDto) {
        try {
            // Basic validation
            String url = connectionDto.buildConnectionUrl();
            String driverClass = connectionDto.type().getDriverClassName();
            
            // Check if driver class is available
            Class.forName(driverClass);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Connection parameters are valid",
                "connectionUrl", url,
                "driverClass", driverClass
            ));
            
        } catch (ClassNotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "Database driver not found: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "Invalid connection parameters: " + e.getMessage()
            ));
        }
    }
}