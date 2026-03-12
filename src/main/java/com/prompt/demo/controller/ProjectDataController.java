package com.prompt.demo.controller;

import com.prompt.demo.dto.request.ProjectDataDTOs;
import com.prompt.demo.dto.response.ApiResponse;
import com.prompt.demo.dto.response.DataSqlExecutionResponse;
import com.prompt.demo.dto.response.ProjectDataResponse;
import com.prompt.demo.entity.ProjectDataTable;
import com.prompt.demo.entity.User;
import com.prompt.demo.service.ProjectDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Project Data", description = "Insert and query sample data for project tables")
public class ProjectDataController {

    private final ProjectDataService projectDataService;

    @GetMapping("/{projectId}/tables")
    @Operation(summary = "List all tables for a project")
    public ResponseEntity<ApiResponse<List<ProjectDataTable>>> listTables(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Tables retrieved",
                projectDataService.getProjectTables(projectId, currentUser.getId())));
    }

    @GetMapping("/{projectId}/tables/{tableName}")
    @Operation(summary = "Get rows from a project table")
    public ResponseEntity<ApiResponse<ProjectDataResponse>> getTableData(
            @PathVariable UUID projectId,
            @PathVariable String tableName,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Data retrieved",
                projectDataService.getTableData(projectId, tableName, currentUser.getId())));
    }

    @PostMapping("/sql")
    @Operation(
            summary = "Execute INSERT / UPDATE / DELETE / SELECT against project tables",
            description = "Use logical table names (e.g. 'users'). They are rewritten to physical names automatically."
    )
    public ResponseEntity<ApiResponse<DataSqlExecutionResponse>> executeDataSql(
            @Valid @RequestBody ProjectDataDTOs.ExecuteDataSqlRequest request,
            @AuthenticationPrincipal User currentUser) {
        DataSqlExecutionResponse result = projectDataService.executeDataSql(request, currentUser.getId());
        String message = "SELECT".equals(result.getType())
                ? "Query returned " + result.getRowsReturned() + " row(s)"
                : result.getType() + " affected " + result.getRowsAffected() + " row(s)";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @DeleteMapping("/{projectId}/clear")
    @Operation(summary = "Clear all data from project tables (keeps structure)")
    public ResponseEntity<ApiResponse<Void>> clearData(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal User currentUser) {
        projectDataService.clearProjectData(projectId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("All data cleared", null));
    }
}