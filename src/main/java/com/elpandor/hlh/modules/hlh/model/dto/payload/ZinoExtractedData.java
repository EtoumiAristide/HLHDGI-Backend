package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Data;

import java.util.Date;

@Data
public class ZinoExtractedData {
    private Date date;
    private String caisse;
    private String modePaiement;
    private Double montantHT;
    private Double tva;
    private Double montantTTC;
}
