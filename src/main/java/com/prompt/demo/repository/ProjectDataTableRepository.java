package com.prompt.demo.repository;

import com.prompt.demo.entity.ProjectDataTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectDataTableRepository extends JpaRepository<ProjectDataTable, UUID> {

    List<ProjectDataTable> findByProjectId(UUID projectId);

    Optional<ProjectDataTable> findByProjectIdAndLogicalName(UUID projectId, String logicalName);

    boolean existsByProjectIdAndLogicalName(UUID projectId, String logicalName);

    @Query("SELECT p.tableName FROM ProjectDataTable p WHERE p.project.id = :projectId")
    List<String> findTableNamesByProjectId(UUID projectId);
}