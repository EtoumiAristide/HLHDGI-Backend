package com.elpandor.hlh.modules.hlh.model.dto.payload.bk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TotauxPayload {
    private Double ht;
    private TauxInfo tdt;
    private TauxInfo tva;
    private Double ttc;
    private String modePaiement;
}
