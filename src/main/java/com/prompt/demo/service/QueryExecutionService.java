package com.prompt.demo.service;

import com.prompt.demo.dto.request.PromptDTOs;
import com.prompt.demo.dto.response.QueryExecutionResponse;
import com.prompt.demo.entity.Prompt;
import com.prompt.demo.entity.QueryExecution;
import com.prompt.demo.entity.User;
import com.prompt.demo.exception.AppExceptions;
import com.prompt.demo.repository.QueryExecutionRepository;
import com.prompt.demo.util.SqlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryExecutionService {

    private final QueryExecutionRepository queryExecutionRepository;
    private final PromptService promptService;
    private final JdbcTemplate jdbcTemplate;
    private final SqlValidator sqlValidator;
    private final ProjectDataService projectDataService; // ← NEW

    @Value("${query.execution.timeout-seconds:30}")
    private int queryTimeoutSeconds;

    @Transactional
    public QueryExecutionResponse executeQuery(PromptDTOs.ExecuteQueryRequest request, User currentUser) {
        Prompt prompt = promptService.getPromptEntity(request.getPromptId(), currentUser.getId());

        if (prompt.getGeneratedSql() == null || prompt.getGeneratedSql().isBlank()) {
            throw new AppExceptions.InvalidSqlException(
                    "No generated SQL found for this prompt. Please generate SQL first."
            );
        }

        String sqlToExecute = request.getCustomSql() != null
                ? request.getCustomSql()
                : prompt.getGeneratedSql();

        // ── NEW: rewrite logical table names → physical table names ──────────
        UUID projectId = prompt.getProject().getId();
        Map<String, String> tableMapping = projectDataService.getTableNameMapping(projectId);
        sqlToExecute = rewriteTableNames(sqlToExecute, tableMapping);
        log.debug("Rewritten SQL: {}", sqlToExecute);
        // ─────────────────────────────────────────────────────────────────────

        String validatedSql = sqlValidator.validateAndPrepare(sqlToExecute);

        QueryExecution execution = QueryExecution.builder()
                .prompt(prompt)
                .executedSql(validatedSql)
                .status(QueryExecution.ExecutionStatus.PENDING)
                .build();

        execution = queryExecutionRepository.save(execution);

        try {
            long startTime = System.currentTimeMillis();
            List<Map<String, Object>> rows = executeWithTimeout(validatedSql);
            long executionTime = System.currentTimeMillis() - startTime;

            execution.setExecutionTime(executionTime);
            execution.setRowsReturned(rows.size());
            execution.setStatus(QueryExecution.ExecutionStatus.SUCCESS);
            execution = queryExecutionRepository.save(execution);

            log.info("Query executed successfully. Rows: {}, Time: {}ms", rows.size(), executionTime);

            List<String> columns = rows.isEmpty()
                    ? List.of()
                    : new ArrayList<>(rows.get(0).keySet());

            return buildResponse(execution, rows, columns, null);

        } catch (AppExceptions.InvalidSqlException e) {
            execution.setStatus(QueryExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            queryExecutionRepository.save(execution);
            throw e;
        } catch (Exception e) {
            String errorMsg = extractUserFriendlyError(e);
            execution.setStatus(QueryExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(errorMsg);
            queryExecutionRepository.save(execution);
            log.error("Query execution failed for prompt {}: {}", prompt.getId(), e.getMessage());
            throw new AppExceptions.QueryExecutionException("Query execution failed: " + errorMsg, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Replace logical names (e.g. "users") with physical names (e.g. "proj_abc12345_users")
    // Uses word-boundary regex to avoid partial replacements
    // ─────────────────────────────────────────────────────────────────────────
    private String rewriteTableNames(String sql, Map<String, String> tableMapping) {
        if (tableMapping == null || tableMapping.isEmpty()) return sql;

        String rewritten = sql;
        for (Map.Entry<String, String> entry : tableMapping.entrySet()) {
            // Match whole word, case-insensitive
            String pattern = "(?i)\\b" + java.util.regex.Pattern.quote(entry.getKey()) + "\\b";
            rewritten = rewritten.replaceAll(pattern, entry.getValue());
        }
        return rewritten;
    }

    private List<Map<String, Object>> executeWithTimeout(String sql) {
        jdbcTemplate.setQueryTimeout(queryTimeoutSeconds);
        return jdbcTemplate.queryForList(sql);
    }

    private String extractUserFriendlyError(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("column") && message.contains("does not exist"))
                return "Column referenced in query does not exist";
            if (message.contains("relation") && message.contains("does not exist"))
                return "Table referenced in query does not exist";
            if (message.contains("syntax error"))
                return "SQL syntax error in generated query";
        }
        return "Query execution failed. Please try regenerating the SQL.";
    }

    private QueryExecutionResponse buildResponse(QueryExecution execution,
                                                 List<Map<String, Object>> rows,
                                                 List<String> columns,
                                                 String errorMessage) {
        return QueryExecutionResponse.builder()
                .id(execution.getId())
                .promptId(execution.getPrompt().getId())
                .executedSql(execution.getExecutedSql())
                .executionTimeMs(execution.getExecutionTime())
                .rowsReturned(execution.getRowsReturned())
                .status(execution.getStatus())
                .errorMessage(errorMessage)
                .rows(rows)
                .columns(columns)
                .createdAt(execution.getCreatedAt())
                .build();
    }
}