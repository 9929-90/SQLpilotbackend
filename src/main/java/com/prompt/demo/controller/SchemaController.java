package com.prompt.demo.controller;


import com.prompt.demo.dto.request.SchemaDTOs;
import com.prompt.demo.dto.response.ApiResponse;
import com.prompt.demo.dto.response.SchemaResponse;
import com.prompt.demo.entity.User;
import com.prompt.demo.service.SchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schema")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Schema", description = "Database schema management")
public class SchemaController {

    private final SchemaService schemaService;

    @PostMapping
    @Operation(summary = "Upload or update schema for a project")
    public ResponseEntity<ApiResponse<SchemaResponse>> saveSchema(
            @Valid @RequestBody SchemaDTOs.CreateSchemaRequest request,
            @AuthenticationPrincipal User currentUser) {
        SchemaResponse response = schemaService.saveSchema(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Schema saved successfully", response));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get schema for a project")
    public ResponseEntity<ApiResponse<SchemaResponse>> getSchema(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal User currentUser) {
        SchemaResponse response = schemaService.getSchema(projectId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}