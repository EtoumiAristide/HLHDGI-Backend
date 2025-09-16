package com.elpandor.hlh.modules.hlh.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.hlh.mapper.FactureMapper;
import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.repository.FactureRepository;
import com.elpandor.hlh.modules.hlh.service.FactureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FactureServiceImpl extends GenericServiceImpl<Facture, Integer, FactureDto> implements FactureService {

    private final FactureMapper factureMapper;
    private final FactureRepository factureRepository;

    public FactureServiceImpl(JpaRepository<Facture, Integer> repository, FactureMapper factureMapper, FactureRepository factureRepository) {
        super(repository);
        this.factureMapper = factureMapper;
        this.factureRepository = factureRepository;
    }

    @Override
    public Facture transformDTOToEntity(FactureDto element) {
        Facture facture = factureMapper.toEntity(element);
        facture.setIsdelete(false);
        return facture;
    }

    @Override
    public FactureDto transformEntityToDTO(Facture element) {
        return factureMapper.toDto(element);
    }

    @Override
    public Page<FactureDto> findByEntreprise(Pageable pageable, String entreprise) {
//        System.out.println("entreprise "+entreprise);
        return factureRepository.findByPointVente_Etablissement_Organisation_RaisonSocial(pageable, entreprise).map(this::transformEntityToDTO);
    }
}
