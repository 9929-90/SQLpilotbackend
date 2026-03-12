package com.prompt.demo.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project_data_tables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDataTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Actual PostgreSQL table name: proj_<shortId>_<logicalName>
    @Column(name = "table_name", nullable = false, length = 200)
    private String tableName;

    // Original name from schema JSON
    @Column(name = "logical_name", nullable = false, length = 100)
    private String logicalName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "columns", nullable = false, columnDefinition = "jsonb")
    private List<String> columns;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();
}
