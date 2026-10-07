package com.elpandor.hlh.modules.impressionzino.application.event;

/**
 * Publié quand de nouveaux jobs sont disponibles pour un établissement.
 * Traité après validation de la transaction ({@code AFTER_COMMIT}) par {@code ImpressionDispatchTask}.
 */
public record PrintJobsCreatedEvent(String codeEtablissement) {
}
