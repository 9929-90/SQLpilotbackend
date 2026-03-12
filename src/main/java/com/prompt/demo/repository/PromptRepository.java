package com.prompt.demo.repository;

import com.prompt.demo.entity.Prompt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, UUID> {

    Page<Prompt> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Prompt> findByUserIdAndProjectIdOrderByCreatedAtDesc(UUID userId, UUID projectId, Pageable pageable);

    @Query("SELECT p FROM Prompt p WHERE p.id = :id AND p.user.id = :userId")
    Optional<Prompt> findByIdAndUserId(UUID id, UUID userId);
}