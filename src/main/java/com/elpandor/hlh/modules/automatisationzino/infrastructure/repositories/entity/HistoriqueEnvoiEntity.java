package com.elpandor.hlh.modules.automatisationzino.infrastructure.repositories.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique_envois")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueEnvoiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "statut", length = 50)
    private String statut;

    @Column(name = "code_erreur", length = 50)
    private String codeErreur;

    @Column(name = "message_erreur", columnDefinition = "TEXT")
    private String messageErreur;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Column(name = "tentative")
    private Integer tentative;

    @Column(name = "reponse_api", columnDefinition = "TEXT")
    private String reponseApi;

    @Column(name = "temps_execution_ms")
    private Long tempsExecutionMs;

    @Column(name = "code_produit_principal", length = 100)
    private String codeProduitPrincipal;

    @PrePersist
    protected void onCreate() {
        if (dateEnvoi == null) {
            dateEnvoi = LocalDateTime.now();
        }
        if (tentative == null) {
            tentative = 0;
        }
    }
}