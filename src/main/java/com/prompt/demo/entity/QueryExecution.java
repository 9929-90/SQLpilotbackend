package com.prompt.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "query_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @Column(name = "executed_sql", nullable = false, columnDefinition = "TEXT")
    private String executedSql;

    @Column(name = "execution_time")
    private Long executionTime;

    @Column(name = "rows_returned")
    private Integer rowsReturned;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public enum ExecutionStatus {
        PENDING, SUCCESS, FAILED
    }
}