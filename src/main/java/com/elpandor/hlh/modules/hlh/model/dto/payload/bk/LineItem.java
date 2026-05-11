package com.elpandor.hlh.modules.hlh.model.dto.payload.bk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LineItem {
    private String date;
    private String produit;
    private Integer quantite;
    private Double prixUnitaireHT;
    private Double montantHT;
}
