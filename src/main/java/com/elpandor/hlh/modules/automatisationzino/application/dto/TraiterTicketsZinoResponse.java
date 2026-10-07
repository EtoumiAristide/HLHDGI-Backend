package com.elpandor.hlh.modules.automatisationzino.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Réponse du webservice de traitement manuel des tickets Zino.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraiterTicketsZinoResponse {

    private boolean succes;
    private String message;
    private String nomFichierSource;
    private int nombreTicketsRecus;
    private String idTransaction;
    private String codeErreur;
    private String details;
    private long tempsExecutionMs;
    private List<Map<String, String>> liensFactureFNE;

    /**
     * Nombre de tickets placés en file d'impression pour le point de vente (impression asynchrone :
     * la réponse n'attend pas l'impression physique, suivie via {@code /api/v1/impression/jobs}).
     */
    private int ticketsEnFileImpression;
}
