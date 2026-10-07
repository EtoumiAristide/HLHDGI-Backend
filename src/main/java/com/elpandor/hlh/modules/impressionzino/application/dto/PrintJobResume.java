package com.elpandor.hlh.modules.impressionzino.application.dto;

import com.elpandor.hlh.modules.impressionzino.infrastructure.PrintJobEntity;

/**
 * Vue allégée d'un job (sans le payload) pour le suivi et l'administration.
 */
public record PrintJobResume(
        String jobId,
        String codeEtablissement,
        String numeroFacture,
        String nomFichierSource,
        String statut,
        int tentatives,
        String agentId,
        String imprimante,
        String dernierMessageErreur,
        String dateCreation,
        String dateImpression) {

    public static PrintJobResume from(PrintJobEntity j) {
        return new PrintJobResume(
                j.getId().toString(),
                j.getCodeEtablissement(),
                j.getNumeroFacture(),
                j.getNomFichierSource(),
                j.getStatut().name(),
                j.getTentatives(),
                j.getAgentId(),
                j.getImprimante(),
                j.getDernierMessageErreur(),
                j.getDateCreation() != null ? j.getDateCreation().toString() : null,
                j.getDateImpression() != null ? j.getDateImpression().toString() : null);
    }
}
