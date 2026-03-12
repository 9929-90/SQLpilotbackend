package com.prompt.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

public class SchemaDTOs {

    @Data
    public static class CreateSchemaRequest {
        @NotNull(message = "Project ID is required")
        private UUID projectId;

        @NotBlank(message = "Schema name is required")
        @Size(max = 100)
        private String name;

        @NotBlank(message = "Schema JSON is required")
        private String schemaJson;
    }
}