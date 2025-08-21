package com.elpandor.hlh.modules.hlh.factures.model.dto.payload;

import lombok.Data;

@Data
public class LigneProduitPayload {
    private String date;
    private String produit;
    private int quantite;
    private double prixUnitaireHT;
    private double montantHT;
}
