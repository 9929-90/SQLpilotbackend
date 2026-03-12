package com.prompt.demo.dto.response;

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
public class SchemaResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String schemaJson;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}