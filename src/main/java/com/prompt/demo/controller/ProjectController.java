package com.prompt.demo.controller;


import com.prompt.demo.dto.request.ProjectDTOs;
import com.prompt.demo.dto.response.ApiResponse;
import com.prompt.demo.dto.response.ProjectResponse;
import com.prompt.demo.entity.User;
import com.prompt.demo.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Projects", description = "Project management")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectDTOs.CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse response = projectService.createProject(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all projects for current user")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects(
            @AuthenticationPrincipal User currentUser) {
        List<ProjectResponse> projects = projectService.getUserProjects(currentUser);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        ProjectResponse project = projectService.getProject(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(project));
    }
}