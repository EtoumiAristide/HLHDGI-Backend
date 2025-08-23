package com.elpandor.hlh.modules.hlh.factures.model.dto.payload;

import com.elpandor.hlh.modules.hlh.factures.model.ModePaiement;
import com.elpandor.hlh.modules.hlh.factures.model.TypeClient;
import com.elpandor.hlh.modules.hlh.factures.model.TypeFacture;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
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
    private TypeClient typeClient;
    private ModePaiement modePaiement;
    private String pointVente;
    private String entreprise;
}
