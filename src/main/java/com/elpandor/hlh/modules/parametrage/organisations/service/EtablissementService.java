package com.elpandor.hlh.modules.parametrage.organisations.service;


import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementDto;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.model.Etablissement;
import com.elpandor.hlh.modules.parametrage.organisations.model.PointVente;

import java.util.List;

public interface EtablissementService extends GenericService<Etablissement, Integer, EtablissementDto> {

    List<EtablissementDto> getAllByOrganosation(Integer organisationId);
    EtablissementDto findByNom(String nom);
}
