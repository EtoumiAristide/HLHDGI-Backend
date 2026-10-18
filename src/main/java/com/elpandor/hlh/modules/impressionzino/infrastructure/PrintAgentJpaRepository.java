package com.elpandor.hlh.modules.impressionzino.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PrintAgentJpaRepository extends JpaRepository<PrintAgentEntity, Long> {

    Optional<PrintAgentEntity> findByApiKeyHashAndActifTrueAndSupprimeFalse(String apiKeyHash);

    Optional<PrintAgentEntity> findByAgentId(String agentId);

    List<PrintAgentEntity> findBySupprimeFalseOrderByDateCreationDesc();

    @Modifying
    @Transactional
    @Query("update PrintAgentEntity a set a.lastSeenAt = :now where a.id = :id")
    void toucher(@Param("id") Long id, @Param("now") LocalDateTime now);
}
