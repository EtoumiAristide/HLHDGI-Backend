package com.elpandor.hlh.modules.hlh.model.dto.payload.hlh;

import lombok.Data;

@Data
public class LigneProduitPayload {
    private String date;
    private String produit;
    private int quantite;
    private String unite;
    private double prixUnitaireHT;
    private double montantHT;
}
