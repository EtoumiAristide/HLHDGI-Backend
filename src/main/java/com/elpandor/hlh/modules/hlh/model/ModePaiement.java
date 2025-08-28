package com.elpandor.hlh.modules.hlh.model;

public enum ModePaiement {

    cash("cash", "Espèce"),
    card("card", "Carte Bancaire"),
    check("check", "Chèque"),
    mobilemoney("mobile-money", "Mobile Money"),
    transfer("transfer", "Virement Bancaire"),
    deferred("deferred", "A Terme");

    private final String value;
    private final String libelle;

    ModePaiement(String value, String libelle) {
        this.value = value;
        this.libelle = libelle;
    }
}
