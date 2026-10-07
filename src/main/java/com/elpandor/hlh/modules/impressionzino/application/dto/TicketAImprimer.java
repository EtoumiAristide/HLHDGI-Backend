package com.elpandor.hlh.modules.impressionzino.application.dto;

/**
 * Ticket prêt à être imprimé : une facture acceptée par la FNE, déjà sérialisée au format
 * attendu par l'agent d'impression (JSON du {@code FactureDto} de l'application desktop).
 *
 * @param numeroFacture numéro de facture (clé d'idempotence du job d'impression)
 * @param typeDocument  type de document, ex. {@code FACTURE_VENTE}
 * @param payloadJson   JSON complet du ticket (lignes, totaux, référence et lien FNE...)
 */
public record TicketAImprimer(String numeroFacture, String typeDocument, String payloadJson) {
}
