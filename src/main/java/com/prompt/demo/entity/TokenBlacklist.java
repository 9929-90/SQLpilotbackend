package com.prompt.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "token_blacklist", indexes = {
        @Index(name = "idx_token_blacklist_token", columnList = "token", unique = true),
        @Index(name = "idx_token_blacklist_expires_at", columnList = "expiresAt")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant revokedAt;
}