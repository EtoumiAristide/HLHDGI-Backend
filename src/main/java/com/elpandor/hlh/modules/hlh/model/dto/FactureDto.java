package com.elpandor.hlh.modules.hlh.model.dto;

import com.elpandor.hlh.modules.hlh.model.ModePaiement;
import com.elpandor.hlh.modules.hlh.model.TypeClient;
import com.elpandor.hlh.modules.hlh.model.TypeFacture;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Builder
@Data
public class FactureDto {
    private Integer id;

    private String reference;
    private String numFacture;
    private LocalDate dateFacture;
    private String nomClient;
    private String lienFichier;

    private String dataSend;
    private String reponseFNE;
    private String bkExtractedData;;

    private TypeFacture typeFacture;
    private TypeClient typeClient;
    private ModePaiement modePaiement;

    private PointVenteDto pointVente;

    private Instant dateCreation;
    private Instant dateModification;
}
