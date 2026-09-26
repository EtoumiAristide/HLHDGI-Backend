package com.elpandor.hlh.modules.parametrage.organisations.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.organisations.mapper.OrganisationMapper;
import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;
import com.elpandor.hlh.modules.parametrage.organisations.repository.OrganisationRepository;
import com.elpandor.hlh.modules.parametrage.organisations.service.OrganisationService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("organisationService")
public class OrganisationServiceImpl extends GenericServiceImpl<Organisation, Integer, OrganisationDto> implements OrganisationService {

    private final OrganisationRepository organisationRepository;


    public OrganisationServiceImpl(JpaRepository<Organisation, Integer> repository, OrganisationRepository organisationRepository) {
        super(repository);
        this.organisationRepository = organisationRepository;
    }

    @Override
    public Organisation transformDTOToEntity(OrganisationDto element) {
        Organisation organisation = OrganisationMapper.INSTANCE.toEntity(element);
        organisation.setIsdelete(false);
        return organisation;
    }

    @Override
    public OrganisationDto transformEntityToDTO(Organisation element) {
        return OrganisationMapper.INSTANCE.toDto(element);
    }

    @Override
    public List<OrganisationDto> getAllByMultipleId(int[] ids) {
        return organisationRepository.getAllByMultipleId(ids).stream().map(this::transformEntityToDTO).toList();
    }

    @Override
    public OrganisationDto findByRaisonSocial(String entreprise) {
        return transformEntityToDTO(organisationRepository.findByRaisonSocial(entreprise));
    }

    @Override
    public OrganisationDto findByNumcc(String numcc) {
        return transformEntityToDTO(organisationRepository.findByNumcc(numcc));
    }

}
