package com.prompt.demo.repository;


import com.prompt.demo.entity.QueryExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueryExecutionRepository extends JpaRepository<QueryExecution, UUID> {
    List<QueryExecution> findByPromptIdOrderByCreatedAtDesc(UUID promptId);
    Optional<QueryExecution> findTopByPromptIdOrderByCreatedAtDesc(UUID promptId);
}