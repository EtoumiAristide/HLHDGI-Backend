package com.elpandor.hlh.modules.hlh.factures.model;

import lombok.Getter;

@Getter
public enum TypeFacture {
    FACTURE_VENTE("FACTURE_VENTE", "Facture de vente"),
    BORDEREAU_ACHAT("BORDEREAU_ACHAT", "Bordereau d'achat"),
    FACTURE_AVOIR("FACTURE_AVOIR", "Facture d'avoir");

    private final String value;
    private final String libelle;

    TypeFacture(String value, String libelle) {
        this.value = value;
        this.libelle = libelle;
    }
}
