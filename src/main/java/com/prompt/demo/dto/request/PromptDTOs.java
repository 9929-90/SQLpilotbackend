package com.prompt.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

public class PromptDTOs {

    @Data
    public static class GeneratePromptRequest {
        @NotNull(message = "Project ID is required")
        private UUID projectId;

        @NotBlank(message = "Prompt text is required")
        private String promptText;
    }

    @Data
    public static class ExecuteQueryRequest {
        @NotNull(message = "Prompt ID is required")
        private UUID promptId;

        private String customSql;
    }
}