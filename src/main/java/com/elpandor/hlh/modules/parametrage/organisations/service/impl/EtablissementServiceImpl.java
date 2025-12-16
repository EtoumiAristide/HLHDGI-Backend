package com.elpandor.hlh.modules.parametrage.organisations.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.parametrage.organisations.dto.EtablissementDto;
import com.elpandor.hlh.modules.parametrage.organisations.mapper.EtablissementMapper;
import com.elpandor.hlh.modules.parametrage.organisations.model.Etablissement;
import com.elpandor.hlh.modules.parametrage.organisations.repository.EtablissementRepository;
import com.elpandor.hlh.modules.parametrage.organisations.service.EtablissementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("etablissementService")
public class EtablissementServiceImpl extends GenericServiceImpl<Etablissement, Integer, EtablissementDto> implements EtablissementService {

    private final EtablissementRepository etablissementRepository;


    @Autowired
    public EtablissementServiceImpl(JpaRepository<Etablissement, Integer> repository, EtablissementRepository etablissementRepository) {
        super(repository);
        this.etablissementRepository = etablissementRepository;
    }

    @Override
    public Etablissement transformDTOToEntity(EtablissementDto element) {
        Etablissement organisation = EtablissementMapper.INSTANCE.toEntity(element);
        organisation.setIsdelete(false);
        return organisation;
    }

    @Override
    public EtablissementDto transformEntityToDTO(Etablissement element) {
        return EtablissementMapper.INSTANCE.toDto(element);
    }

    @Override
    public List<EtablissementDto> getAllByOrganosation(Integer organisationId) {
        return etablissementRepository.findByOrganisationId(organisationId).stream().map(this::transformEntityToDTO).toList();
    }

    @Override
    public EtablissementDto findByNom(String nom) {
        return transformEntityToDTO(etablissementRepository.findByNomContainingIgnoreCase(nom));
    }
}
