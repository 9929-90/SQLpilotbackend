package com.prompt.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProjectDataDTOs {

    // ── Existing: manual row insert ───────────────────────────────────────────
    @Data
    public static class InsertRowsRequest {
        @NotNull
        private UUID projectId;

        @NotBlank
        private String tableName;

        @NotNull
        private List<Map<String, Object>> rows;
    }

    @Data
    public static class BulkInsertRequest {
        @NotNull
        private UUID projectId;

        @NotNull
        private Map<String, List<Map<String, Object>>> data;
    }

    // ── NEW: raw SQL execution against project tables ─────────────────────────
    @Data
    public static class ExecuteDataSqlRequest {
        @NotNull
        private UUID projectId;

        @NotBlank
        private String sql;
    }
}