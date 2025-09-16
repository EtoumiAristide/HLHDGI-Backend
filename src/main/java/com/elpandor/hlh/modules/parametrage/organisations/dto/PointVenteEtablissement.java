package com.elpandor.hlh.modules.parametrage.organisations.dto;

import lombok.Data;

import java.util.List;

@Data
public class PointVenteEtablissement {
    private EtablissementDto etablissement;
    private List<PointVenteDto> pointVentes;
}
