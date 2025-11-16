package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class TotauxPayload {
    private double ht;
    private TaxePayload tdt;
    private TaxePayload tva;
    private double ttc;
    private String modePaiement;
}
