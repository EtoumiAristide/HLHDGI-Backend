package com.elpandor.hlh.modules.hlh.model.dto.payload.bk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BKExtractedData {
    private String numeroFacture;
    private String dateFacture;
    private ClientPayload clientPayload;
    private List<LineItem> lignes;
    private TotauxPayload totauxPayload;
    private String typeFacture;
    private String typeClient;
    private String modePaiement;
    private String pointVente;
    private String entreprise;
    private String sheetName;
    
    // Legacy fields (kept for backward compatibility if needed)
    private List<Payment> payments;
    private List<Comp> comps;
    private List<Promo> promos;
}
