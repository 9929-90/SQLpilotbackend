package com.prompt.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class ProjectDTOs {

    @Data
    public static class CreateProjectRequest {
        @NotBlank(message = "Project name is required")
        @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
        private String name;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;
    }
}