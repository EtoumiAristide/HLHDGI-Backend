package com.elpandor.hlh.modules.parametrage.organisations.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.mapper.CompteUtilisateurMapper;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.CompteUtilisateur;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.model.dto.CompteUtilisateurDto;
import com.elpandor.hlh.modules.parametrage.compteutilisateur.repository.CompteUtilisateurRepository;
import com.elpandor.hlh.modules.parametrage.organisations.dto.OrganisationDto;
import com.elpandor.hlh.modules.parametrage.organisations.mapper.OrganisationMapper;
import com.elpandor.hlh.modules.parametrage.organisations.model.Organisation;
import com.elpandor.hlh.modules.parametrage.organisations.repository.OrganisationRepository;
import com.elpandor.hlh.modules.parametrage.organisations.service.OrganisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service("organisationService")
public class OrganisationServiceImpl extends GenericServiceImpl<Organisation, Integer, OrganisationDto> implements OrganisationService {

    private final CompteUtilisateurRepository organisationUtilisateurRepository;
    private final CompteUtilisateurMapper organisationUtilisateurMapper;
    private final OrganisationRepository organisationRepository;


    @Autowired
    public OrganisationServiceImpl(JpaRepository<Organisation, Integer> repository, CompteUtilisateurRepository organisationUtilisateurRepository, CompteUtilisateurMapper organisationUtilisateurMapper,
                                   OrganisationRepository organisationRepository) {
        super(repository);
        this.organisationUtilisateurRepository = organisationUtilisateurRepository;
        this.organisationUtilisateurMapper = organisationUtilisateurMapper;
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
    public CompteUtilisateurDto getOrganisationByUtilisateur(UUID utilisateurId) {
        CompteUtilisateur organisationUtilisateur = organisationUtilisateurRepository.findByKeycloakUserId(utilisateurId.toString());

        if (organisationUtilisateur != null) {
            return organisationUtilisateurMapper.toDto(organisationUtilisateur);
        }
        return null;
    }

    @Override
    public List<OrganisationDto> getAllByMultipleId(int[] ids) {
        return organisationRepository.getAllByMultipleId(ids).stream().map(this::transformEntityToDTO).toList();
    }

    @Override
    public OrganisationDto findByRaisonSocial(String entreprise) {
        return transformEntityToDTO(organisationRepository.findByRaisonSocial(entreprise));
    }

}
