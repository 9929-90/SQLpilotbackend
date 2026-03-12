package com.prompt.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prompt.demo.dto.request.SchemaDTOs;
import com.prompt.demo.dto.response.SchemaResponse;
import com.prompt.demo.entity.Project;
import com.prompt.demo.entity.Schema;
import com.prompt.demo.entity.User;
import com.prompt.demo.exception.AppExceptions;
import com.prompt.demo.repository.SchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaService {

    private final SchemaRepository schemaRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;
    private final ProjectDataService projectDataService; // ← NEW

    @Transactional
    @CacheEvict(value = "schemas", key = "#request.projectId")
    public SchemaResponse saveSchema(SchemaDTOs.CreateSchemaRequest request, User currentUser) {
        Project project = projectService.getProjectEntity(request.getProjectId(), currentUser.getId());

        validateSchemaJson(request.getSchemaJson());

        Schema schema = schemaRepository.findByProjectId(project.getId())
                .orElse(Schema.builder().project(project).build());

        schema.setName(request.getName());
        schema.setSchemaJson(request.getSchemaJson());
        schema = schemaRepository.save(schema);

        // ── NEW: recreate real PostgreSQL tables from schema ──────────────────
        projectDataService.recreateTablesFromSchema(project.getId(), request.getSchemaJson());
        // ─────────────────────────────────────────────────────────────────────

        log.info("Schema saved and tables recreated for project: {}", project.getId());
        return toResponse(schema);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "schemas", key = "#projectId")
    public SchemaResponse getSchema(UUID projectId, User currentUser) {
        projectService.getProjectEntity(projectId, currentUser.getId());

        Schema schema = schemaRepository.findByProjectId(projectId)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException(
                        "Schema not found for project: " + projectId
                ));

        return toResponse(schema);
    }

    public String getSchemaJson(UUID projectId) {
        return schemaRepository.findByProjectId(projectId)
                .map(Schema::getSchemaJson)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException(
                        "Schema not found for project: " + projectId +
                                ". Please upload a schema first."
                ));
    }

    private void validateSchemaJson(String schemaJson) {
        try {
            objectMapper.readTree(schemaJson);
        } catch (Exception e) {
            throw new AppExceptions.InvalidSqlException("Invalid schema JSON format: " + e.getMessage());
        }
    }

    private SchemaResponse toResponse(Schema schema) {
        return SchemaResponse.builder()
                .id(schema.getId())
                .projectId(schema.getProject().getId())
                .name(schema.getName())
                .schemaJson(schema.getSchemaJson())
                .createdAt(schema.getCreatedAt())
                .updatedAt(schema.getUpdatedAt())
                .build();
    }
}