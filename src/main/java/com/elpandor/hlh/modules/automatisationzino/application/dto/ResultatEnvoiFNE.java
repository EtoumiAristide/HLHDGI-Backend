package com.elpandor.hlh.modules.automatisationzino.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

 // Résultat normalisé de l'envoi d'une facture Zino à la FNE via ApimService.Remplace l'ancien FNEResponseDTO du batch (qui supposait un envoi de fichier multipart, mécanisme non utilisé en réalité).

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultatEnvoiFNE {
    private boolean succes;
    private String reponseBrute;
    private String codeErreur;
    private String message;
    private String idTransaction;
    private String details;
}