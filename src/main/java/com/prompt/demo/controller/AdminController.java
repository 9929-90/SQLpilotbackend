package com.prompt.demo.controller;

import com.prompt.demo.dto.response.ApiResponse;
import com.prompt.demo.dto.response.UserSummaryResponse;
import com.prompt.demo.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")          // entire controller is ADMIN-only
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin", description = "Admin-only user management")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> listUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<UserSummaryResponse> users = adminService.listUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @PatchMapping("/users/{id}/toggle-status")
    @Operation(summary = "Toggle a user's active/inactive status")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> toggleStatus(
            @PathVariable UUID id) {

        UserSummaryResponse updated = adminService.toggleStatus(id);
        return ResponseEntity.ok(ApiResponse.success("User status updated", updated));
    }
}