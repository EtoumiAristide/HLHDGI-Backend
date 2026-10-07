package com.elpandor.hlh.modules.impressionzino.infrastructure;

import com.elpandor.hlh.modules.impressionzino.domain.PrintJobStatut;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Job d'impression (table "outbox") : un ticket à imprimer sur le poste d'un point de vente.
 * <p>
 * Remarque : {@code fichier_source_id} est volontairement une simple colonne, sans clé étrangère vers
 * {@code fichiers_source}. Les jobs sont créés dans une transaction indépendante (REQUIRES_NEW) alors
 * que la ligne {@code fichiers_source} n'est pas encore validée par la transaction du traitement.
 */
@Entity
@Table(name = "print_jobs",
        uniqueConstraints = @UniqueConstraint(name = "uk_print_jobs_numero_facture", columnNames = "numero_facture"),
        indexes = {
                @Index(name = "idx_print_jobs_routage", columnList = "code_etablissement, statut, available_at"),
                @Index(name = "idx_print_jobs_fichier", columnList = "fichier_source_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Clé de routage : code établissement enregistré dans {@code fichiers_source}, normalisé (trim + majuscules). */
    @Column(name = "code_etablissement", nullable = false, length = 50)
    private String codeEtablissement;

    @Column(name = "fichier_source_id")
    private Long fichierSourceId;

    @Column(name = "nom_fichier_source", length = 255)
    private String nomFichierSource;

    @Column(name = "numero_facture", nullable = false, length = 100)
    private String numeroFacture;

    @Column(name = "type_document", nullable = false, length = 30)
    private String typeDocument;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private PrintJobStatut statut;

    @Column(name = "tentatives", nullable = false)
    private int tentatives;

    /** Le job n'est livrable qu'à partir de cette date (délai avant nouvelle tentative). */
    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    /** Fin du bail : sans accusé de réception avant cette date, le job est remis en file. */
    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    @Column(name = "agent_id", length = 100)
    private String agentId;

    @Column(name = "imprimante", length = 150)
    private String imprimante;

    @Column(name = "dernier_message_erreur", columnDefinition = "TEXT")
    private String dernierMessageErreur;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_derniere_modification")
    private LocalDateTime dateDerniereModification;

    @Column(name = "date_impression")
    private LocalDateTime dateImpression;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        dateCreation = now;
        dateDerniereModification = now;
        if (statut == null) {
            statut = PrintJobStatut.PENDING;
        }
        if (availableAt == null) {
            availableAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        dateDerniereModification = LocalDateTime.now();
    }
}
