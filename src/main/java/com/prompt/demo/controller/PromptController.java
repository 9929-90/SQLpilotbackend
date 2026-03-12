package com.prompt.demo.controller;


import com.prompt.demo.dto.request.PromptDTOs;
import com.prompt.demo.dto.response.ApiResponse;
import com.prompt.demo.dto.response.PromptResponse;
import com.prompt.demo.entity.User;
import com.prompt.demo.service.PromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Prompts", description = "AI SQL generation from natural language")
public class PromptController {

    private final PromptService promptService;

    @PostMapping("/generate")
    @Operation(summary = "Generate SQL from natural language prompt")
    public ResponseEntity<ApiResponse<PromptResponse>> generateSql(
            @Valid @RequestBody PromptDTOs.GeneratePromptRequest request,
            @AuthenticationPrincipal User currentUser) {
        PromptResponse response = promptService.generateSql(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("SQL generated successfully", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get prompt history for current user")
    public ResponseEntity<ApiResponse<Page<PromptResponse>>> getHistory(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        Page<PromptResponse> history = promptService.getHistory(currentUser, projectId, page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific prompt by ID")
    public ResponseEntity<ApiResponse<PromptResponse>> getPrompt(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        PromptResponse response = promptService.getPrompt(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}