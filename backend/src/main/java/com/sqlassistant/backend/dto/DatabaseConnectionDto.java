package com.sqlassistant.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DatabaseConnectionDto(
    @NotNull
    DatabaseType type,
    
    @NotBlank
    String host,
    
    Integer port,
    
    @NotBlank
    String database,
    
    String username,
    
    String password,
    
    String connectionString
) {
    public enum DatabaseType {
        MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://", 3306),
        POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://", 5432),
        SQLITE("org.sqlite.JDBC", "jdbc:sqlite:", null),
        H2("org.h2.Driver", "jdbc:h2:", null);
        
        private final String driverClassName;
        private final String urlPrefix;
        private final Integer defaultPort;
        
        DatabaseType(String driverClassName, String urlPrefix, Integer defaultPort) {
            this.driverClassName = driverClassName;
            this.urlPrefix = urlPrefix;
            this.defaultPort = defaultPort;
        }
        
        public String getDriverClassName() { return driverClassName; }
        public String getUrlPrefix() { return urlPrefix; }
        public Integer getDefaultPort() { return defaultPort; }
    }
    
    public String buildConnectionUrl() {
        if (connectionString != null && !connectionString.isBlank()) {
            return connectionString;
        }
        
        return switch (type) {
            case MYSQL, POSTGRESQL -> {
                int actualPort = port != null ? port : type.getDefaultPort();
                yield type.getUrlPrefix() + host + ":" + actualPort + "/" + database;
            }
            case SQLITE -> type.getUrlPrefix() + database;
            case H2 -> type.getUrlPrefix() + database;
        };
    }
}