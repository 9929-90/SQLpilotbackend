package com.prompt.demo.repository;

import com.prompt.demo.entity.Schema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchemaRepository extends JpaRepository<Schema, UUID> {
    Optional<Schema> findByProjectId(UUID projectId);
    boolean existsByProjectId(UUID projectId);
    void deleteByProjectId(UUID projectId);
}