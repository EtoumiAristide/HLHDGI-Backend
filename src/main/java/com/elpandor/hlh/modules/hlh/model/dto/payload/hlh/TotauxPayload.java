package com.elpandor.hlh.modules.hlh.model.dto.payload.hlh;

import lombok.Data;

@Data
public class TotauxPayload {
    private double ht;
    private TaxePayload tdt;
    private TaxePayload tva;
    private TaxePayload tcn;
    private double ttc;
    private String modePaiement;
}
