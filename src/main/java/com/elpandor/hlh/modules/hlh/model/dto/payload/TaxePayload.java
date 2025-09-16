package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Data;

@Data
public class TaxePayload {
    private double base;
    private double montant;
    private Double taux; // Optional pour TDT qui n'a pas de taux dans le fichier
}
