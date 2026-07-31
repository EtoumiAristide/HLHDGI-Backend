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
public class HistoriqueEnvoi {
    private Long id;
    private String nomFichier;
    private StatutEnvoi statut;
    private String codeErreur;
    private String messageErreur;
    private LocalDateTime dateEnvoi;
    private Integer tentative;
    private String reponseApi;
    private Long tempsExecutionMs;
    private String codeProduitPrincipal;
}
