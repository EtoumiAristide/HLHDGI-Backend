package com.elpandor.hlh.modules.parametrage.organisations.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.organisations.dto.PointVenteDto;
import com.elpandor.hlh.modules.parametrage.organisations.mapper.PointVenteMapper;
import com.elpandor.hlh.modules.parametrage.organisations.model.PointVente;
import com.elpandor.hlh.modules.parametrage.organisations.repository.PointVenteRepository;
import com.elpandor.hlh.modules.parametrage.organisations.service.PointVenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("pointventeService")
public class PointVenteServiceImpl extends GenericServiceImpl<PointVente, Integer, PointVenteDto> implements PointVenteService {

    private final PointVenteRepository pointVenteRepository;


    @Autowired
    public PointVenteServiceImpl(JpaRepository<PointVente, Integer> repository, PointVenteRepository pointVenteRepository) {
        super(repository);
        this.pointVenteRepository = pointVenteRepository;
    }

    @Override
    public PointVente transformDTOToEntity(PointVenteDto element) {
        PointVente organisation = PointVenteMapper.INSTANCE.toEntity(element);
        organisation.setIsdelete(false);
        return organisation;
    }

    @Override
    public PointVenteDto transformEntityToDTO(PointVente element) {
        return PointVenteMapper.INSTANCE.toDto(element);
    }

    @Override
    public List<PointVenteDto> getAllByEtablissement(Integer etablissementId) {
        return pointVenteRepository.findByEtablissementId(etablissementId).stream().map(this::transformEntityToDTO).toList();
    }

    @Override
    public List<PointVenteDto> getAllByOrganisation(Integer organisationId) {
        return pointVenteRepository.findByEtablissementOrganisationId(organisationId).stream().map(this::transformEntityToDTO).toList();
    }

    @Override
    public PointVenteDto findByNom(String nom) {
        return transformEntityToDTO(pointVenteRepository.findByNom(nom));
    }

    @Override
    public List<PointVenteDto> getAllByOrganisationName(String raisonSocial) {
        return pointVenteRepository.findByEtablissementOrganisationRaisonSocial(raisonSocial).stream().map(this::transformEntityToDTO).toList();
    }
}
