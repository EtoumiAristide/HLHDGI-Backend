package com.elpandor.hlh.modules.automatisationzino.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Ligne de détail du rapport d'état des extractions : une facture individuelle envoyée à la FNE
 * dans le cadre de l'automatisation Zino.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportFactureDto {
    private String numFacture;
    private String referenceFNE;
    private LocalDate dateFacture;
    private String nomClient;
    private String modePaiement;
    private Double montant;
    private String statutFNE;
    private String nomFichierSource;
    private String lienFacture;
}
