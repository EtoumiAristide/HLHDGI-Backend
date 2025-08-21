package com.elpandor.hlh.modules.parametrage.organisations.dto;

import lombok.Data;

import java.util.List;

@Data
public class PointVenteEntreprise {
    private OrganisationDto organisation;
    private List<PointVenteDto> pointVentes;
}
