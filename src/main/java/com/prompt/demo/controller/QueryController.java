package com.prompt.demo.controller;

import com.prompt.demo.dto.request.PromptDTOs;
import com.prompt.demo.dto.response.ApiResponse;
import com.prompt.demo.dto.response.QueryExecutionResponse;
import com.prompt.demo.entity.User;
import com.prompt.demo.service.QueryExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Query Execution", description = "Execute generated SQL queries safely")
public class QueryController {

    private final QueryExecutionService queryExecutionService;

    @PostMapping("/execute")
    @Operation(summary = "Execute a generated SQL query",
            description = "Executes the SQL for a given prompt. Only SELECT and WITH queries are allowed.")
    public ResponseEntity<ApiResponse<QueryExecutionResponse>> executeQuery(
            @Valid @RequestBody PromptDTOs.ExecuteQueryRequest request,
            @AuthenticationPrincipal User currentUser) {
        QueryExecutionResponse response = queryExecutionService.executeQuery(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Query executed successfully", response));
    }
}