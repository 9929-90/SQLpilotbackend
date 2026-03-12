package com.prompt.demo.dto.response;

import com.prompt.demo.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class UserSummaryResponse {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private String status;       // "active" | "inactive" — derived from User.enabled
    private ZonedDateTime joinedAt;

    public static UserSummaryResponse from(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getDisplayUsername())   // the actual username field, not getUsername() which returns email
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.isEnabled() ? "active" : "inactive")
                .joinedAt(user.getCreatedAt())
                .build();
    }
}