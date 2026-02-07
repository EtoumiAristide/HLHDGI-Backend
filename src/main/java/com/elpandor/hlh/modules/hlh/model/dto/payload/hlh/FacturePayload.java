package com.elpandor.hlh.modules.hlh.model.dto.payload.hlh;

import com.elpandor.hlh.modules.hlh.model.ModePaiement;
import com.elpandor.hlh.modules.hlh.model.TypeClient;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import lombok.*;

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
    private TypeClient typeClient;
    private ModePaiement modePaiement;
    private String pointVente;
    private String entreprise;
    private String sheetName;
    private double pourcentageTVA;
    private double pourcentageTDT;
    private double valeurTCN;
}
