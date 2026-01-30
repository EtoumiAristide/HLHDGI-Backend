package com.elpandor.hlh.modules.hlh.model.dto;

import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Builder
@Data
public class FactureLoadDto {
    private UUID id;

    private String lienFichier;

    private String dataFacture;
    private String bkExtractedData;;

    private PointVenteDto pointVente;

    private Instant dateCreation;
    private Instant dateModification;
}
