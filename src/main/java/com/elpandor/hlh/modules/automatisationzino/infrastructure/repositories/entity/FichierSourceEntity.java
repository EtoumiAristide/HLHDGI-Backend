package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fichiers_source")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FichierSourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_fichier", nullable = false, length = 255, unique = true)
    private String nomFichier;

    @Column(name = "chemin_acces", length = 500)
    private String cheminAcces;

    @Column(name = "statut", length = 50)
    private String statut;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_derniere_modification")
    private LocalDateTime dateDerniereModification;

    @Column(name = "tentative_envoi")
    private Integer tentativeEnvoi;

    @Column(name = "dernier_message_erreur", columnDefinition = "TEXT")
    private String dernierMessageErreur;

    @Column(name = "code_produit_principal", length = 100)
    private String codeProduitPrincipal;

    // ⚠️ NOUVEAUX CHAMPS
    @Column(name = "donnees_extraites_json", columnDefinition = "TEXT")
    private String donneesExtraitesJson;

    @Column(name = "extraction_effectuee")
    private Boolean extractionEffectuee;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateDerniereModification = LocalDateTime.now();
        if (tentativeEnvoi == null) {
            tentativeEnvoi = 0;
        }
        if (statut == null) {
            statut = "PENDING";
        }
        if (extractionEffectuee == null) {
            extractionEffectuee = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateDerniereModification = LocalDateTime.now();
    }
}