package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class LigneProduitPayload {
    private String date;
    private String produit;
    private int quantite;
    private double prixUnitaireHT;
    private double montantHT;
}
