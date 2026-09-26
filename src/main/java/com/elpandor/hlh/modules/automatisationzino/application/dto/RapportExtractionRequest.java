package com.elpandor.hlh.modules.automatisationzino.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RapportExtractionRequest {

    @NotNull
    private LocalDate dateDebut;

    @NotNull
    private LocalDate dateFin;

    /** Filtre optionnel sur le statut du fichier (PENDING, SENT, ERROR, ECHEC_DEFINITIF). Null/vide = tous. */
    private String statut;
}
