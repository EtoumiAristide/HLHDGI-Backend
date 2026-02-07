package com.elpandor.hlh.modules.hlh.model.dto.payload.bk;

import lombok.Data;

@Data
public class BKExtratedData2 {

    private String date;
    private String reference;
    private String checkNumber;
    private Double ht;      // Hors Taxe
    private Double tdt;     // TDT (2.5%)
    private Double tva;     // TVA (18%)
    private Double amountTTC; // Montant TTC
}
