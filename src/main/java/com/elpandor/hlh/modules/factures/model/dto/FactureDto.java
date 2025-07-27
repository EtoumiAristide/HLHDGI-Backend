package com.elpandor.hlh.modules.factures.model.dto;

import com.elpandor.hlh.modules.factures.model.TypeFacture;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Builder
@Data
public class FactureDto {
    private Integer id;

    private String numFacture;
    private LocalDate dateFacture;
    private String nomClient;
    private String lienFichier;

    private String dataSend;
    private String reponseFNE;

    private TypeFacture typeFacture;

    private Instant dateCreation;
    private Instant dateModification;
}
