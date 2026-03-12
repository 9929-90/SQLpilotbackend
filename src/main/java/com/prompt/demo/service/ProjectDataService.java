package com.prompt.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prompt.demo.dto.request.ProjectDataDTOs;
import com.prompt.demo.dto.response.DataSqlExecutionResponse;
import com.prompt.demo.dto.response.ProjectDataResponse;
import com.prompt.demo.entity.Project;
import com.prompt.demo.entity.ProjectDataTable;
import com.prompt.demo.exception.AppExceptions;
import com.prompt.demo.repository.ProjectDataTableRepository;
import com.prompt.demo.util.SqlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectDataService {

    private final ProjectDataTableRepository projectDataTableRepository;
    private final ProjectService projectService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SqlValidator sqlValidator;

    // ─────────────────────────────────────────────────────────────────────────
    // Called from SchemaService — recreate physical tables
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void recreateTablesFromSchema(UUID projectId, String schemaJson) {
        List<Map<String, Object>> tables = parseTablesFromSchema(schemaJson);
        if (tables == null || tables.isEmpty()) return;

        dropProjectTables(projectId);

        String prefix = projectId.toString().replace("-", "").substring(0, 8);

        for (Map<String, Object> tabledef : tables) {
            String logicalName = (String) tabledef.get("name");
            List<String> columns = castToStringList(tabledef.get("columns"));
            if (logicalName == null || columns == null || columns.isEmpty()) continue;

            String physicalName = "proj_" + prefix + "_" + logicalName.toLowerCase();
            createPhysicalTable(physicalName, columns);

            ProjectDataTable entry = ProjectDataTable.builder()
                    .project(Project.builder().id(projectId).build())
                    .tableName(physicalName)
                    .logicalName(logicalName)
                    .columns(columns)
                    .build();

            projectDataTableRepository.save(entry);
            log.info("Created table {} for project {}", physicalName, projectId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Execute raw SQL typed by user (INSERT/UPDATE/DELETE/SELECT)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public DataSqlExecutionResponse executeDataSql(
            ProjectDataDTOs.ExecuteDataSqlRequest request, UUID userId) {

        projectService.getProjectEntity(request.getProjectId(), userId);

        String validated = sqlValidator.validateDataMutation(request.getSql());

        Map<String, String> mapping = getTableNameMapping(request.getProjectId());
        String rewritten = rewriteTableNames(validated, mapping);
        log.debug("Data SQL rewritten: {}", rewritten);

        String upperSql = rewritten.trim().toUpperCase();
        long start = System.currentTimeMillis();

        if (upperSql.startsWith("SELECT") || upperSql.startsWith("WITH")) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(rewritten);
            long elapsed = System.currentTimeMillis() - start;
            List<String> columns = rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
            return DataSqlExecutionResponse.builder()
                    .executedSql(rewritten)
                    .type("SELECT")
                    .rowsReturned(rows.size())
                    .columns(columns)
                    .rows(rows)
                    .executionTimeMs(elapsed)
                    .build();
        }

        String type = upperSql.startsWith("INSERT") ? "INSERT"
                : upperSql.startsWith("UPDATE") ? "UPDATE" : "DELETE";

        int affected = jdbcTemplate.update(rewritten);
        long elapsed = System.currentTimeMillis() - start;

        log.info("{} affected {} row(s) in {}ms", type, affected, elapsed);

        return DataSqlExecutionResponse.builder()
                .executedSql(rewritten)
                .type(type)
                .rowsAffected(affected)
                .rowsReturned(0)
                .columns(List.of())
                .rows(List.of())
                .executionTimeMs(elapsed)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read / clear
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProjectDataResponse getTableData(UUID projectId, String logicalName, UUID userId) {
        projectService.getProjectEntity(projectId, userId);
        ProjectDataTable table = projectDataTableRepository
                .findByProjectIdAndLogicalName(projectId, logicalName)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException(
                        "Table '" + logicalName + "' not found."));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + table.getTableName() + " LIMIT 500");
        return ProjectDataResponse.builder()
                .tableName(logicalName)
                .physicalTable(table.getTableName())
                .columns(table.getColumns())
                .rows(rows)
                .rowCount(rows.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ProjectDataTable> getProjectTables(UUID projectId, UUID userId) {
        projectService.getProjectEntity(projectId, userId);
        return projectDataTableRepository.findByProjectId(projectId);
    }

    @Transactional
    public void clearProjectData(UUID projectId, UUID userId) {
        projectService.getProjectEntity(projectId, userId);
        projectDataTableRepository.findByProjectId(projectId).forEach(t -> {
            jdbcTemplate.execute("DELETE FROM " + t.getTableName());
            log.info("Cleared {}", t.getTableName());
        });
    }

    public Map<String, String> getTableNameMapping(UUID projectId) {
        return projectDataTableRepository.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(
                        ProjectDataTable::getLogicalName,
                        ProjectDataTable::getTableName));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────
    private String rewriteTableNames(String sql, Map<String, String> mapping) {
        if (mapping == null || mapping.isEmpty()) return sql;
        String rewritten = sql;
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String pattern = "(?i)\\b" + java.util.regex.Pattern.quote(entry.getKey()) + "\\b";
            rewritten = rewritten.replaceAll(pattern, entry.getValue());
        }
        return rewritten;
    }

    private void createPhysicalTable(String tableName, List<String> columns) {
        String colsDef = columns.stream()
                .map(c -> "\"" + c.toLowerCase() + "\" TEXT")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" + colsDef + ")");
    }

    private void dropProjectTables(UUID projectId) {
        List<ProjectDataTable> existing = projectDataTableRepository.findByProjectId(projectId);
        existing.forEach(t -> jdbcTemplate.execute("DROP TABLE IF EXISTS " + t.getTableName()));
        projectDataTableRepository.deleteAll(existing);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseTablesFromSchema(String schemaJson) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(schemaJson, new TypeReference<>() {});
            return (List<Map<String, Object>>) parsed.get("tables");
        } catch (Exception e) {
            log.error("Failed to parse schema JSON: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> castToStringList(Object obj) {
        if (obj instanceof List<?>) return (List<String>) obj;
        return List.of();
    }
}