package com.elpandor.hlh.modules.impressionzino.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Agent d'impression = une installation de l'application desktop sur le poste d'un point de vente.
 * Il est rattaché à UN code établissement ; la clé d'API n'est jamais stockée en clair (hash SHA-256).
 */
@Entity
@Table(name = "print_agents",
        uniqueConstraints = @UniqueConstraint(name = "uk_print_agents_agent_id", columnNames = "agent_id"),
        indexes = @Index(name = "idx_print_agents_cle", columnList = "api_key_hash", unique = true))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintAgentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false, length = 100)
    private String agentId;

    @Column(name = "code_etablissement", nullable = false, length = 50)
    private String codeEtablissement;

    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    /** Dernier contact de l'agent (chaque poll le met à jour) : sert à détecter un poste hors ligne. */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}
