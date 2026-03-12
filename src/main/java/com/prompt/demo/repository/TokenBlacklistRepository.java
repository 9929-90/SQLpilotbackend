package com.prompt.demo.repository;

import com.prompt.demo.entity.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {

    boolean existsByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < :now")
    void deleteAllExpiredBefore(Instant now);
}