package com.elpandor.hlh.modules.hlh.factures.model;

public enum TypeClient {
    B2B("B2B", "Business To Business"),
    B2F("B2F", "Business To Fund"),
    B2G("B2G", "Business To Government"),
    B2C("B2C", "Business To Customer");

    private final String value;
    private final String libelle;

    TypeClient(String value, String libelle) {
        this.value = value;
        this.libelle = libelle;
    }
}
