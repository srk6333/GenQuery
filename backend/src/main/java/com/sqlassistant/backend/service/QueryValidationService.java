package com.sqlassistant.backend.service;

import com.sqlassistant.backend.dto.QueryDto;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class QueryValidationService {
    
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
        "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE",
        "EXEC", "EXECUTE", "SP_", "XP_", "SCRIPT", "SHUTDOWN", "BULK"
    );
    
    private static final Set<String> ALLOWED_SELECT_KEYWORDS = Set.of(
        "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER",
        "ON", "GROUP", "BY", "HAVING", "ORDER", "LIMIT", "OFFSET", "UNION",
        "AND", "OR", "NOT", "IN", "EXISTS", "LIKE", "BETWEEN", "IS", "NULL",
        "DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX", "AS", "CASE", "WHEN",
        "THEN", "ELSE", "END", "CAST", "CONVERT"
    );
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('(''|[^'])*')|(;)|(\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT|SELECT|UNION|UPDATE)\\b)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
        "(--[^\r\n]*)|(/\\*[\\w\\W]*?(?=\\*/)\\*/)", Pattern.CASE_INSENSITIVE
    );
    
    public QueryDto.QueryValidationResponse validateQuery(String sql) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        if (sql == null || sql.trim().isEmpty()) {
            errors.add("Query cannot be null or empty");
            return new QueryDto.QueryValidationResponse(false, errors, warnings, suggestions, null);
        }
        
        String cleanedSql = cleanQuery(sql);
        
        // Basic syntax validation
        try {
            Statement statement = CCJSqlParserUtil.parse(cleanedSql);
            validateStatementType(statement, errors, warnings);
            validateQueryComplexity(statement, warnings, suggestions);
            
        } catch (JSQLParserException e) {
            errors.add("SQL syntax error: " + e.getMessage());
        }
        
        // Security validation
        validateForSQLInjection(cleanedSql, errors, warnings);
        validateDangerousOperations(cleanedSql, errors, warnings);
        
        // Performance suggestions
        generatePerformanceSuggestions(cleanedSql, suggestions);
        
        boolean isValid = errors.isEmpty();
        String sanitizedQuery = isValid ? sanitizeQuery(cleanedSql) : null;
        
        return new QueryDto.QueryValidationResponse(
            isValid, errors, warnings, suggestions, sanitizedQuery
        );
    }
    
    private String cleanQuery(String sql) {
        // Remove comments
        String cleaned = COMMENT_PATTERN.matcher(sql).replaceAll(" ");
        
        // Normalize whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    private void validateStatementType(Statement statement, List<String> errors, List<String> warnings) {
        if (statement instanceof Select) {
            // SELECT statements are generally safe
            return;
        }
        
        if (statement instanceof Insert || statement instanceof Update || statement instanceof Delete) {
            errors.add("Data modification queries (INSERT/UPDATE/DELETE) are not allowed in this context");
            return;
        }
        
        if (statement instanceof Drop || statement instanceof Truncate) {
            errors.add("Destructive operations (DROP/TRUNCATE) are strictly forbidden");
            return;
        }
        
        warnings.add("Query type could not be determined or may not be safe");
    }
    
    private void validateQueryComplexity(Statement statement, List<String> warnings, List<String> suggestions) {
        String sql = statement.toString().toUpperCase();
        
        // Count nested subqueries
        int subqueryCount = (sql.split("\\(\\s*SELECT", -1).length - 1);
        if (subqueryCount > 3) {
            warnings.add("Query has many nested subqueries (" + subqueryCount + "), which may impact performance");
            suggestions.add("Consider breaking down complex subqueries into simpler parts or using CTEs");
        }
        
        // Count joins
        int joinCount = (sql.split("\\s+JOIN\\s+", -1).length - 1);
        if (joinCount > 5) {
            warnings.add("Query has many joins (" + joinCount + "), which may impact performance");
            suggestions.add("Consider if all joins are necessary and ensure proper indexing");
        }
        
        // Check for cartesian products
        if (sql.contains("FROM") && sql.contains(",") && !sql.contains("WHERE")) {
            warnings.add("Query may contain cartesian product - missing JOIN conditions");
        }
    }
    
    private void validateForSQLInjection(String sql, List<String> errors, List<String> warnings) {
        String upperSql = sql.toUpperCase();
        
        // Check for suspicious patterns
        if (upperSql.contains("1=1") || upperSql.contains("'='")) {
            warnings.add("Query contains suspicious patterns that may indicate SQL injection");
        }
        
        // Check for multiple statements
        if (sql.split(";").length > 1) {
            errors.add("Multiple statements in a single query are not allowed");
        }
        
        // Check for dangerous functions
        String[] dangerousFunctions = {"EXEC", "SP_", "XP_", "OPENROWSET", "OPENDATASOURCE"};
        for (String func : dangerousFunctions) {
            if (upperSql.contains(func)) {
                errors.add("Query contains dangerous function: " + func);
            }
        }
    }
    
    private void validateDangerousOperations(String sql, List<String> errors, List<String> warnings) {
        String upperSql = sql.toUpperCase();
        
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                if (keyword.equals("SELECT")) continue; // SELECT is allowed
                
                errors.add("Query contains dangerous operation: " + keyword);
            }
        }
        
        // Check for administrative operations
        String[] adminOperations = {"GRANT", "REVOKE", "CREATE USER", "DROP USER", "ALTER USER"};
        for (String op : adminOperations) {
            if (upperSql.contains(op)) {
                errors.add("Administrative operations are not permitted: " + op);
            }
        }
    }
    
    private void generatePerformanceSuggestions(String sql, List<String> suggestions) {
        String upperSql = sql.toUpperCase();
        
        // Check for SELECT *
        if (upperSql.contains("SELECT *")) {
            suggestions.add("Consider specifying column names instead of using SELECT * for better performance");
        }
        
        // Check for missing WHERE clause
        if (upperSql.contains("SELECT") && !upperSql.contains("WHERE") && !upperSql.contains("LIMIT")) {
            suggestions.add("Consider adding a WHERE clause or LIMIT to avoid scanning entire tables");
        }
        
        // Check for functions in WHERE clause
        if (upperSql.matches(".*WHERE.*\\w+\\([^)]*\\).*")) {
            suggestions.add("Using functions in WHERE clause may prevent index usage");
        }
        
        // Check for LIKE with leading wildcard
        if (upperSql.contains("LIKE '%") || upperSql.contains("LIKE \"'%")) {
            suggestions.add("LIKE patterns starting with wildcard (%) cannot use indexes efficiently");
        }
        
        // Check for ORDER BY without LIMIT
        if (upperSql.contains("ORDER BY") && !upperSql.contains("LIMIT")) {
            suggestions.add("Consider adding LIMIT when using ORDER BY to improve performance");
        }
    }
    
    private String sanitizeQuery(String sql) {
        // Remove any remaining comments
        String sanitized = COMMENT_PATTERN.matcher(sql).replaceAll(" ");
        
        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        
        // Ensure query ends with semicolon
        if (!sanitized.endsWith(";")) {
            sanitized += ";";
        }
        
        return sanitized;
    }
    
    public boolean isReadOnlyQuery(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return statement instanceof Select;
        } catch (JSQLParserException e) {
            return false; // If we can't parse it, assume it's not read-only
        }
    }
    
    public Set<String> extractTableNames(String sql) {
        Set<String> tableNames = new HashSet<>();
        
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select select) {
                // This would require a more sophisticated visitor pattern implementation
                // For now, return empty set
            }
        } catch (JSQLParserException e) {
            // Fallback to simple regex-based extraction
            Pattern tablePattern = Pattern.compile("\\bFROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
            var matcher = tablePattern.matcher(sql);
            while (matcher.find()) {
                tableNames.add(matcher.group(1));
            }
        }
        
        return tableNames;
    }
}