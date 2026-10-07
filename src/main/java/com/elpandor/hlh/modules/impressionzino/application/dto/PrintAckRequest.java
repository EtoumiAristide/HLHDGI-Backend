package com.elpandor.hlh.modules.impressionzino.application.dto;

/**
 * Accusé de réception envoyé par l'agent après tentative d'impression.
 *
 * @param statut     {@code PRINTED} ou {@code FAILED}
 * @param imprimante nom de l'imprimante utilisée (optionnel)
 * @param message    détail de l'erreur en cas d'échec (optionnel)
 */
public record PrintAckRequest(String statut, String imprimante, String message) {
}
