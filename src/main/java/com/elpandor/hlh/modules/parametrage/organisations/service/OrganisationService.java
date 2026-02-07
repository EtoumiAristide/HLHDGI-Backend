package com.elpandor.hlh.modules.parametrage.organisations.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;

import java.util.List;
import java.util.UUID;

public interface OrganisationService extends GenericService<Organisation, Integer, OrganisationDto> {

    List<OrganisationDto> getAllByMultipleId(int[] ids);

    OrganisationDto findByRaisonSocial(String entreprise);
    //    void updateIsPrincipal();
}
