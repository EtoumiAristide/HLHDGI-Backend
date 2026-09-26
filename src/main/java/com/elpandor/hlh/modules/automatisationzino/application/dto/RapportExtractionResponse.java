package com.elpandor.hlh.modules.automatisationzino.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportExtractionResponse {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private List<RapportFichierDto> fichiers;
//    private List<RapportFactureDto> factures;
    private RapportExtractionTotaux totaux;
}
