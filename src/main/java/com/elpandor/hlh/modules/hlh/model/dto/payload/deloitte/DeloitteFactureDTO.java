package com.elpandor.hlh.modules.hlh.model.dto.payload.deloitte;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DeloitteFactureDTO {
    private String nomClient; // ALSTOM METRO D’ABIDJAN, MI OVERSEAS LIMITED, etc.
    private String numeroFacture; // N°25 / 2542, N°26 / 1138, etc.
    private List<ItemFacture> items; // Liste des articles/services
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private String tauxTVA; // "18%", "non facturée", "suspendue", etc.
    private BigDecimal montantTTC;
    private String devise; // XOF ou EUR
    private String dateFacture;
    private String referenceInterne;
    private String adresseClient;
    private String contactClient;
    private String bonDeCommande;
    private Boolean estPDFScanne;

    @Data
    public static class ItemFacture {
        private String description;
        private BigDecimal montant;
        private String devise;
    }
}
