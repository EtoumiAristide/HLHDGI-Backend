package com.elpandor.hlh.modules.automatisationzino.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportExtractionTotaux {
    private int nombreFichiers;
    private int nombreFichiersSucces;
    private int nombreFichiersErreur;
    private int nombreFichiersEnAttente;
    private int nombreTicketsExtraits;
    private int nombreFacturesEnvoyees;
}
