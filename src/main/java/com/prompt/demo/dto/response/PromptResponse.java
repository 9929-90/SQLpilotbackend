package com.prompt.demo.dto.response;

import com.prompt.demo.entity.Prompt;
import com.prompt.demo.entity.QueryExecution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponse {
    private UUID id;
    private UUID userId;
    private UUID projectId;
    private String promptText;
    private String generatedSql;
    private String explanation;
    private Prompt.PromptStatus status;
    private ZonedDateTime createdAt;
    private QueryExecutionSummary lastExecution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryExecutionSummary {
        private UUID id;
        private Long executionTime;
        private Integer rowsReturned;
        private QueryExecution.ExecutionStatus status;
        private ZonedDateTime createdAt;
    }
}