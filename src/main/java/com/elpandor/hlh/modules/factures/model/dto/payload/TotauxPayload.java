package com.elpandor.hlh.modules.factures.model.dto.payload;

import lombok.Data;

@Data
public class TotauxPayload {
    private double ht;
    private TaxePayload tdt;
    private TaxePayload tva;
    private double ttc;
    private String modePaiement;
}
