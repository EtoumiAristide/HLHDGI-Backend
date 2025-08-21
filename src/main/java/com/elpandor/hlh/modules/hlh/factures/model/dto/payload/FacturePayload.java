package com.elpandor.hlh.modules.hlh.factures.model.dto.payload;

import com.elpandor.hlh.modules.hlh.factures.model.TypeFacture;
import lombok.Data;

import java.util.List;

@Data
public class FacturePayload {
    private String numeroFacture;
    private String dateFacture;
    private ClientPayload clientPayload;
    private List<LigneProduitPayload> lignes;
    private TotauxPayload totauxPayload;
    private String reception;
    private TypeFacture typeFacture;
}
