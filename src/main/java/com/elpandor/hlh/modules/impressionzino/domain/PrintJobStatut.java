package com.elpandor.hlh.modules.impressionzino.domain;

/**
 * Cycle de vie d'un job d'impression.
 * <pre>
 *  PENDING ──(agent réserve)──► DISPATCHED ──(ack PRINTED)──► PRINTED
 *     ▲                             │
 *     └──(bail expiré / ack FAILED, tentatives restantes)
 *                                   └──(tentatives épuisées)──► FAILED
 * </pre>
 */
public enum PrintJobStatut {
    /** En attente d'un agent (ou d'une prochaine tentative). */
    PENDING,
    /** Remis à un agent, accusé de réception attendu avant expiration du bail. */
    DISPATCHED,
    /** Imprimé, accusé reçu. */
    PRINTED,
    /** Échec définitif (nombre maximal de tentatives atteint) : relance manuelle possible. */
    FAILED
}
