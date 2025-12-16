package com.elpandor.hlh.modules.hlh.model.dto.payload.deloitte;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeloitteFacturePayload {
    private String typeDocument; // "FACTURE" ou "AVOIR"
    private String numeroDocument;
    private String referenceInterne;
    private String date;
    private String devise; // EUR ou XOF
    private BigDecimal montantHT;
    private BigDecimal montantDebours;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private String numeroFactureAnnulee; // Pour les avoirs
    private String description;
    private String clientDestinataire;
    private String modalitePaiement;
    private String banque;
    private String rib;
    private String swift;
    private String adresse;
    private Boolean estPDFScanne; // Nouveau champ
}
