package com.elpandor.hlh.modules.parametrage.organisations.dto;

import lombok.Data;

import java.util.List;

@Data
public class EtablissementEntreprise {
    private OrganisationDto organisation;
    private List<EtablissementDto> etablissements;
}
