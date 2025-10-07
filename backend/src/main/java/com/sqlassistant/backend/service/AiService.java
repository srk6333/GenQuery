package com.sqlassistant.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlassistant.backend.dto.QueryDto;
import com.sqlassistant.backend.model.DatabaseSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AiService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    @Value("${openai.api.base-url}")
    private String openAiBaseUrl;
    
    @Value("${openai.api.model}")
    private String model;
    
    public AiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }


    public QueryDto.QueryResponse generateSqlQuery(
            String naturalLanguageQuery,
            DatabaseSchema schema,
            Map<String, Object> context) {

        String GEMINI_API_KEY = "your key";
        String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isBlank()) {
            return createErrorResponse("Gemini API key not configured");
        }

        try {
            // ✅ Build prompt
            String systemPrompt = buildSystemPrompt(schema);
            String userPrompt = buildUserPrompt(naturalLanguageQuery, context);
            String prompt = systemPrompt + "\n\n\n" + userPrompt;

            // ✅ Build request body
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> partsItem = Map.of("parts", List.of(textPart));
            Map<String, Object> requestData = Map.of("contents", List.of(partsItem));

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestData);

            // ✅ Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", GEMINI_API_KEY);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            RestTemplate restTemplate = new RestTemplate();

            // ✅ Send POST request
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    GEMINI_API_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // ✅ Check for success
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                String responseBody = responseEntity.getBody();
                System.out.println("✅ Gemini API Response: " + responseBody);

                return parseGeminiResponse(responseBody);
            } else {
                System.err.println("❌ Gemini API returned error: " + responseEntity.getStatusCode());
                return createErrorResponse("Gemini API returned status " + responseEntity.getStatusCodeValue());
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("❌ Gemini API HTTP Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return createErrorResponse("Gemini API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            System.err.println("⏰ Gemini API Timeout or Connection Error: " + e.getMessage());
            return createErrorResponse("Gemini API timeout or connection error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }



    private String buildSystemPrompt(DatabaseSchema schema) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a SQL query assistant. Generate safe, read-only SQL queries based on natural language requests.\n\n");
        
        prompt.append("IMPORTANT RULES:\n");
        prompt.append("- Generate ONLY SELECT queries\n");
        prompt.append("- Never generate INSERT, UPDATE, DELETE, DROP, CREATE, ALTER, or any data-modifying queries\n");
        prompt.append("- Always use proper JOIN syntax instead of comma joins\n");
        prompt.append("- Include appropriate WHERE clauses to limit results\n");
        prompt.append("- Use meaningful column aliases for better readability\n");
        prompt.append("- Consider adding LIMIT clauses for large result sets\n\n");
        
        prompt.append("DATABASE SCHEMA:\n");
        prompt.append("Database: ").append(schema.databaseName()).append("\n\n");
        
        // Add table information
        for (DatabaseSchema.TableInfo table : schema.tables()) {
            prompt.append("TABLE: ").append(table.name()).append("\n");
            if (table.comment() != null && !table.comment().isEmpty()) {
                prompt.append("Description: ").append(table.comment()).append("\n");
            }
            
            prompt.append("Columns:\n");
            for (DatabaseSchema.ColumnInfo column : table.columns()) {
                prompt.append("  - ").append(column.name())
                      .append(" (").append(column.columnType()).append(")");
                
                if (column.isPrimaryKey()) prompt.append(" [PRIMARY KEY]");
                if (!column.nullable()) prompt.append(" [NOT NULL]");
                if (column.isAutoIncrement()) prompt.append(" [AUTO_INCREMENT]");
                if (column.comment() != null && !column.comment().isEmpty()) {
                    prompt.append(" - ").append(column.comment());
                }
                prompt.append("\n");
            }
            
            // Add foreign key information
            if (!table.foreignKeys().isEmpty()) {
                prompt.append("Foreign Keys:\n");
                for (DatabaseSchema.ForeignKeyInfo fk : table.foreignKeys()) {
                    prompt.append("  - ").append(fk.columnName())
                          .append(" → ").append(fk.referencedTable())
                          .append(".").append(fk.referencedColumn()).append("\n");
                }
            }
            prompt.append("\n");
        }
        
        // Add view information
        if (!schema.views().isEmpty()) {
            prompt.append("VIEWS:\n");
            for (DatabaseSchema.ViewInfo view : schema.views()) {
                prompt.append("VIEW: ").append(view.name()).append("\n");
                if (view.comment() != null && !view.comment().isEmpty()) {
                    prompt.append("Description: ").append(view.comment()).append("\n");
                }
                prompt.append("Columns: ");
                for (DatabaseSchema.ColumnInfo column : view.columns()) {
                    prompt.append(column.name()).append(" (").append(column.columnType()).append("), ");
                }
                prompt.append("\n\n");
            }
        }
        
        prompt.append("Respond with a JSON object containing:\n");
        prompt.append("- sql: the generated SQL query\n");
        prompt.append("- explanation: brief explanation of what the query does\n");
        prompt.append("- warnings: array of any warnings about the query\n");
        
        return prompt.toString();
    }
    
    private String buildUserPrompt(String naturalLanguageQuery, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Natural language query: ").append(naturalLanguageQuery).append("\n\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("Additional context:\n");
            context.forEach((key, value) -> 
                prompt.append("- ").append(key).append(": ").append(value).append("\n"));
        }
        
        prompt.append("\nGenerate a SQL query that answers this request. ");
        prompt.append("Ensure the query is safe, efficient, and follows best practices.");
        
        return prompt.toString();
    }

    private QueryDto.QueryResponse parseGeminiResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);

            if (rootNode.has("candidates") && rootNode.get("candidates").isArray()) {
                JsonNode candidate = rootNode.get("candidates").get(0);
                JsonNode parts = candidate.path("content").path("parts");

                if (parts.isArray() && !parts.isEmpty()) {
                    String generatedText = parts.get(0).path("text").asText();

                    // Clean markdown code fences
                    String cleanedText = Pattern.compile("(?s)```json\\s*").matcher(generatedText).replaceAll("");
                    cleanedText = Pattern.compile("(?s)```").matcher(cleanedText).replaceAll("");
                    cleanedText = cleanedText.trim();

                    try {
                        JsonNode contentJson = mapper.readTree(cleanedText);

                        String sql = contentJson.path("sql").asText("");
                        String explanation = contentJson.path("explanation").asText("");
                        List<String> warnings = new ArrayList<>();

                        if (contentJson.has("warnings") && contentJson.get("warnings").isArray()) {
                            for (JsonNode w : contentJson.get("warnings")) {
                                warnings.add(w.asText());
                            }
                        }

                        QueryDto.QueryMetadata metadata = new QueryDto.QueryMetadata(
                                "SELECT",
                                Collections.emptyList(),
                                false,
                                false,
                                false,
                                "AI_GENERATED",
                                java.time.LocalDateTime.now()
                        );

                        return new QueryDto.QueryResponse(sql, explanation, warnings, true, metadata);

                    } catch (Exception jsonEx) {
                        System.err.println("⚠️ Generated text is not valid JSON: " + cleanedText);
                        return new QueryDto.QueryResponse(
                                cleanedText,
                                "Generated SQL query (raw text)",
                                Collections.emptyList(),
                                true,
                                new QueryDto.QueryMetadata(
                                        "SELECT", Collections.emptyList(), false, false, false,
                                        "AI_GENERATED", java.time.LocalDateTime.now()
                                )
                        );
                    }
                }
            }

            return createErrorResponse("No valid candidates found in Gemini response");

        } catch (Exception e) {
            return createErrorResponse("Failed to parse Gemini response: " + e.getMessage());
        }
    }

    private QueryDto.QueryResponse parseOpenAiResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidatesNode = rootNode.get("candidates");

            if (candidatesNode != null && candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                JsonNode firstCandidate = candidatesNode.get(0);
                JsonNode contentNode = firstCandidate.path("content");
                JsonNode partsNode = contentNode.path("parts");

                if (partsNode.isArray() && !partsNode.isEmpty()) {
                    String content = partsNode.get(0).path("text").asText();

                    // Strip Markdown code block wrapper (```json ... ```)
                    content = content.replaceAll("(?s)```json\\s*", "")
                            .replaceAll("(?s)```", "")
                            .trim();

                    try {
                        JsonNode contentJson = objectMapper.readTree(content);

                        String sql = contentJson.path("sql").asText("");
                        String explanation = contentJson.path("explanation").asText("");
                        List<String> warnings = Collections.emptyList();

                        if (contentJson.has("warnings") && contentJson.get("warnings").isArray()) {
                            warnings = objectMapper.convertValue(
                                    contentJson.get("warnings"),
                                    objectMapper.getTypeFactory()
                                            .constructCollectionType(List.class, String.class)
                            );
                        }

                        QueryDto.QueryMetadata metadata = new QueryDto.QueryMetadata(
                                "SELECT",
                                Collections.emptyList(),
                                false,
                                false,
                                false,
                                "AI_GENERATED",
                                LocalDateTime.now()
                        );

                        return new QueryDto.QueryResponse(sql, explanation, warnings, true, metadata);

                    } catch (Exception e) {
                        // If not JSON, try to extract SQL manually
                        String sql = extractSqlFromText(content);
                        return new QueryDto.QueryResponse(
                                sql,
                                "Generated SQL query",
                                Collections.emptyList(),
                                true,
                                new QueryDto.QueryMetadata(
                                        "SELECT",
                                        Collections.emptyList(),
                                        false,
                                        false,
                                        false,
                                        "AI_GENERATED",
                                        LocalDateTime.now()
                                )
                        );
                    }
                }
            }
        } catch (Exception e) {
            return createErrorResponse("Failed to parse AI response: " + e.getMessage());
        }

        return createErrorResponse("No valid response received from AI service");
    }


    private String extractSqlFromText(String text) {
        // Look for SQL code blocks
        String[] codeBlockMarkers = {"```sql", "```", "SELECT"};
        
        for (String marker : codeBlockMarkers) {
            int startIndex = text.indexOf(marker);
            if (startIndex != -1) {
                int endIndex;
                if (marker.equals("```sql") || marker.equals("```")) {
                    startIndex += marker.length();
                    endIndex = text.indexOf("```", startIndex);
                    if (endIndex == -1) endIndex = text.length();
                } else {
                    endIndex = text.length();
                }
                
                String sql = text.substring(startIndex, endIndex).trim();
                if (sql.toUpperCase().startsWith("SELECT")) {
                    return sql;
                }
            }
        }
        
        // If no code blocks found, return the entire text (cleaned up)
        return text.trim();
    }
    
    private QueryDto.QueryResponse createErrorResponse(String error) {
        return new QueryDto.QueryResponse(
            null, 
            error, 
            List.of("AI service unavailable or returned invalid response"), 
            false, 
            new QueryDto.QueryMetadata(
                "ERROR", Collections.emptyList(), false, false, false, "ERROR", LocalDateTime.now()
            )
        );
    }
    
    public Mono<String> explainQuery(String sql, DatabaseSchema schema) {
        String systemPrompt = "You are a SQL expert. Explain SQL queries in simple, clear language.";
        String userPrompt = "Explain this SQL query: " + sql;
        
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.3,
            "max_tokens", 500
        );
        
        return webClient.post()
            .uri(openAiBaseUrl + "/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::extractExplanationFromResponse)
            .onErrorReturn("Unable to generate explanation");
    }
    
    private String extractExplanationFromResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode choicesNode = rootNode.get("choices");
            
            if (choicesNode != null && choicesNode.isArray() && !choicesNode.isEmpty()) {
                return choicesNode.get(0).get("message").get("content").asText();
            }
        } catch (Exception e) {
            // Fall through to default return
        }
        
        return "Unable to generate explanation";
    }
}