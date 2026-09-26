package com.elpandor.hlh.modules.automatisationzino.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FichierSource {
    private Long id;
    private String nomFichier;
    private String cheminAcces;
    private String statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereModification;
    private Integer tentativeEnvoi;
    private String dernierMessageErreur;
    private String codeProduitPrincipal;

    private String donneesExtraitesJson;
    private Boolean extractionEffectuee;
}