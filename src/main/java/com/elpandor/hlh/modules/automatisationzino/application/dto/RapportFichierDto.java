package com.elpandor.hlh.modules.automatisationzino.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ligne de synthèse du rapport d'état des extractions : un fichier Zino traité par le batch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportFichierDto {
    private Long id;
    private String nomFichier;
    private String statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereModification;
    private Integer tentativeEnvoi;
    private String dernierMessageErreur;
    private Integer nombreTickets;
    private Integer nombreFacturesEnvoyees;
}
