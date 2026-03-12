package com.prompt.demo.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DataSqlExecutionResponse {
    private String executedSql;       // rewritten physical SQL
    private String type;              // SELECT | INSERT | UPDATE | DELETE
    private Integer rowsAffected;     // for INSERT/UPDATE/DELETE
    private Integer rowsReturned;     // for SELECT
    private List<String> columns;     // for SELECT
    private List<Map<String, Object>> rows; // for SELECT
    private long executionTimeMs;
}