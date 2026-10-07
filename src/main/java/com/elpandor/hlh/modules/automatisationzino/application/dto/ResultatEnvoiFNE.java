package com.elpandor.hlh.modules.automatisationzino.application.dto;

import com.elpandor.hlh.modules.impressionzino.application.dto.TicketAImprimer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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
    private List<Map<String, String>> liensFactureFNE;

    /**
     * Une entrée par facture acceptée par la FNE, avec le ticket à imprimer (payload JSON).
     * Alimente la file d'impression ({@code PrintJobService}) ; vide ou {@code null} si rien à imprimer.
     */
    private List<TicketAImprimer> facturesImprimables;
}