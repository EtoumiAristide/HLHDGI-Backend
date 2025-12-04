package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Data;

import java.util.Date;

@Data
public class ZinoExtractedDataOrdered {
    private Date date;
    private String modePaiement;
    private Double totalMontantTTC;
    private Double totalMontantHT;
    private Double totalTVA;
    private int nombreTransactions;
}
