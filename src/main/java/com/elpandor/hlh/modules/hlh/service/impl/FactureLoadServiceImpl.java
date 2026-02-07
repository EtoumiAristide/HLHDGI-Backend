package com.elpandor.hlh.modules.hlh.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.hlh.mapper.FactureLoadMapper;
import com.elpandor.hlh.modules.hlh.mapper.FactureMapper;
import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.hlh.model.FactureLoad;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.model.dto.FactureLoadDto;
import com.elpandor.hlh.modules.hlh.repository.FactureLoadRepository;
import com.elpandor.hlh.modules.hlh.repository.FactureRepository;
import com.elpandor.hlh.modules.hlh.service.FactureLoadService;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FactureLoadServiceImpl extends GenericServiceImpl<FactureLoad, UUID, FactureLoadDto> implements FactureLoadService {

    private final FactureLoadMapper factureMapper;
    private final FactureLoadRepository factureRepository;

    public FactureLoadServiceImpl(JpaRepository<FactureLoad, UUID> repository, FactureLoadMapper factureMapper, FactureLoadRepository factureRepository) {
        super(repository);
        this.factureMapper = factureMapper;
        this.factureRepository = factureRepository;
    }

    @Override
    public FactureLoad transformDTOToEntity(FactureLoadDto element) {
        FactureLoad facture = factureMapper.toEntity(element);
        facture.setIsdelete(false);
        return facture;
    }

    @Override
    public FactureLoadDto transformEntityToDTO(FactureLoad element) {
        return factureMapper.toDto(element);
    }

    @Override
    public Page<FactureLoadDto> findByEntreprise(Pageable pageable, String entreprise) {
//        System.out.println("entreprise "+entreprise);
        return factureRepository.findByPointVente_Etablissement_Organisation_RaisonSocialOrderByIdDesc(pageable, entreprise).map(this::transformEntityToDTO);
    }
}
