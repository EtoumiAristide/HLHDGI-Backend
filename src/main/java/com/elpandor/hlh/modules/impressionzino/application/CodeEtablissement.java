package com.elpandor.hlh.modules.impressionzino.application;

import java.util.Locale;

/**
 * Normalisation du code établissement, utilisée à l'écriture (jobs) comme à la lecture (agents),
 * pour que « e001 », « E001 » et « E001 » (avec espaces) désignent le même point de vente.
 */
public final class CodeEtablissement {

    private CodeEtablissement() {
    }

    /** @return le code normalisé (trim + majuscules), ou {@code null} s'il est vide. */
    public static String normaliser(String code) {
        if (code == null) {
            return null;
        }
        String propre = code.trim();
        return propre.isEmpty() ? null : propre.toUpperCase(Locale.ROOT);
    }
}
