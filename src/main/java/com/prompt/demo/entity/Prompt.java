package com.prompt.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "generated_sql", columnDefinition = "TEXT")
    private String generatedSql;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PromptStatus status = PromptStatus.PENDING;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public enum PromptStatus {
        PENDING, SUCCESS, FAILED
    }
}