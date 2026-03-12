package com.prompt.demo.dto.response;

import com.prompt.demo.entity.QueryExecution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryExecutionResponse {
    private UUID id;
    private UUID promptId;
    private String executedSql;
    private Long executionTimeMs;
    private Integer rowsReturned;
    private QueryExecution.ExecutionStatus status;
    private String errorMessage;
    private List<Map<String, Object>> rows;
    private List<String> columns;
    private ZonedDateTime createdAt;
}