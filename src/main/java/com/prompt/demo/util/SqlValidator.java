package com.prompt.demo.util;

import com.prompt.demo.exception.AppExceptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class SqlValidator {

    @Value("${query.execution.max-rows:100}")
    private int maxRows;

    // ── Destructive DDL — blocked everywhere ──────────────────────────────────
    private static final List<String> ALWAYS_BLOCKED = List.of(
            "DROP", "TRUNCATE", "ALTER", "CREATE",
            "REPLACE", "EXEC", "EXECUTE", "GRANT", "REVOKE", "CALL"
    );

    // ── Allowed starts for read-only queries ─────────────────────────────────
    private static final List<String> READ_ONLY_START = List.of("SELECT", "WITH");

    // ── Allowed starts for data mutation queries ──────────────────────────────
    private static final List<String> DATA_MUTATION_START = List.of("INSERT", "UPDATE", "DELETE", "SELECT", "WITH");

    private static final Pattern SEMICOLON_INJECTION = Pattern.compile(";.*\\S", Pattern.DOTALL);

    // ─────────────────────────────────────────────────────────────────────────
    // Used by QueryExecutionService — SELECT / WITH only
    // ─────────────────────────────────────────────────────────────────────────
    public String validateAndPrepare(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new AppExceptions.InvalidSqlException("SQL query cannot be empty");
        }

        String normalized = sql.trim().toUpperCase();

        boolean startsWithAllowed = READ_ONLY_START.stream().anyMatch(normalized::startsWith);
        if (!startsWithAllowed) {
            throw new AppExceptions.InvalidSqlException("Only SELECT and WITH queries are allowed");
        }

        checkAlwaysBlocked(sql);
        checkSemicolonInjection(sql);

        return addLimitIfNeeded(sql);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Used by ProjectDataService — INSERT / UPDATE / DELETE / SELECT allowed
    // DDL (DROP, ALTER, CREATE, TRUNCATE …) still blocked
    // ─────────────────────────────────────────────────────────────────────────
    public String validateDataMutation(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new AppExceptions.InvalidSqlException("SQL query cannot be empty");
        }

        String normalized = sql.trim().toUpperCase();

        boolean startsWithAllowed = DATA_MUTATION_START.stream().anyMatch(normalized::startsWith);
        if (!startsWithAllowed) {
            throw new AppExceptions.InvalidSqlException(
                    "Only INSERT, UPDATE, DELETE, and SELECT statements are allowed here"
            );
        }

        checkAlwaysBlocked(sql);
        checkSemicolonInjection(sql);

        // Add LIMIT only for SELECT queries
        if (normalized.startsWith("SELECT") || normalized.startsWith("WITH")) {
            return addLimitIfNeeded(sql);
        }

        return sql.trim().replaceAll(";$", "");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void checkAlwaysBlocked(String sql) {
        for (String blocked : ALWAYS_BLOCKED) {
            Pattern p = Pattern.compile("\\b" + blocked + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(sql).find()) {
                throw new AppExceptions.InvalidSqlException(
                        "Blocked SQL keyword: " + blocked
                );
            }
        }
    }

    private void checkSemicolonInjection(String sql) {
        if (SEMICOLON_INJECTION.matcher(sql).find()) {
            throw new AppExceptions.InvalidSqlException(
                    "Multiple SQL statements are not allowed"
            );
        }
    }

    private String addLimitIfNeeded(String sql) {
        String upperSql = sql.toUpperCase().trim();
        String cleanSql = sql.trim().replaceAll(";$", "");
        if (!upperSql.contains("LIMIT")) {
            cleanSql = cleanSql + " LIMIT " + maxRows;
            log.debug("Added LIMIT {} to query", maxRows);
        }
        return cleanSql;
    }
}