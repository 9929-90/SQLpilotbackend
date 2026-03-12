package com.prompt.demo.ai;

import com.prompt.demo.exception.AppExceptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Retryable(
            retryFor = {AppExceptions.AiServiceException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public GeminiResponse generateSql(String schemaJson, String userPrompt) {
        String prompt = buildPrompt(schemaJson, userPrompt);
        log.debug("Calling Gemini API for prompt: {}", userPrompt);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "topK", 1,
                            "topP", 1,
                            "maxOutputTokens", 2048
                    ),
                    "safetySettings", List.of(
                            Map.of(
                                    "category", "HARM_CATEGORY_DANGEROUS_CONTENT",
                                    "threshold", "BLOCK_ONLY_HIGH"
                            )
                    )
            );

            String urlWithKey = apiUrl + "?key=" + apiKey;
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    urlWithKey, request, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseGeminiResponse(response.getBody());
            } else {
                throw new AppExceptions.AiServiceException(
                        "Gemini API returned non-OK status: " + response.getStatusCode()
                );
            }

        } catch (AppExceptions.AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Gemini API: {}", e.getMessage(), e);
            throw new AppExceptions.AiServiceException("Failed to generate SQL: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String schemaJson, String userPrompt) {
        return """
                You are an expert SQL developer. Your task is to generate accurate SQL queries based on the provided database schema.
                
                DATABASE SCHEMA:
                %s
                
                USER REQUEST:
                %s
                
                INSTRUCTIONS:
                1. Generate a valid SQL SELECT query that satisfies the user's request
                2. Use proper JOIN syntax when querying multiple tables
                3. Include appropriate WHERE, GROUP BY, ORDER BY clauses as needed
                4. Do NOT include any DML statements (INSERT, UPDATE, DELETE)
                5. Do NOT include any DDL statements (CREATE, DROP, ALTER)
                
                Respond ONLY with a valid JSON object in this exact format (no markdown, no explanation outside JSON):
                {
                  "sql_query": "YOUR_SQL_QUERY_HERE",
                  "explanation": "Brief explanation of what the query does"
                }
                """.formatted(schemaJson, userPrompt);
    }

    private GeminiResponse parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty()) {
                throw new AppExceptions.AiServiceException("No candidates in Gemini response");
            }

            String text = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // Clean markdown code blocks if present
            text = text.trim()
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode parsed = objectMapper.readTree(text);
            String sql = parsed.path("sql_query").asText();
            String explanation = parsed.path("explanation").asText();

            if (sql.isBlank()) {
                throw new AppExceptions.AiServiceException("Gemini returned empty SQL query");
            }

            return new GeminiResponse(sql, explanation);

        } catch (AppExceptions.AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new AppExceptions.AiServiceException("Failed to parse AI response", e);
        }
    }

    public record GeminiResponse(String sqlQuery, String explanation) {}
}