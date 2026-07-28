package com.elpandor.hlh.modules.parametrage.organisations.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;
import com.elpandor.hlh.modules.parametrage.organisations.model.PointVente;

import java.util.List;
import java.util.UUID;

public interface PointVenteService extends GenericService<PointVente, Integer, PointVenteDto> {

    List<PointVenteDto> getAllByEtablissement(Integer etablissementId);
    List<PointVenteDto> getAllByOrganisation(Integer organisationId);
    PointVenteDto findByNom(String nom);
    List<PointVenteDto> getAllByOrganisationName(String raisonSocial);
}
