package com.elpandor.hlh.modules.hlh.factures.service.impl;

import com.elpandor.hlh.common.core.base.GenericServiceImpl;
import com.elpandor.hlh.modules.hlh.factures.mapper.FactureMapper;
import com.elpandor.hlh.modules.hlh.factures.model.Facture;
import com.elpandor.hlh.modules.hlh.factures.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.factures.service.FactureService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class FactureServiceImpl extends GenericServiceImpl<Facture, Integer, FactureDto> implements FactureService {

    private final FactureMapper factureMapper;
    public FactureServiceImpl(JpaRepository<Facture, Integer> repository, FactureMapper factureMapper) {
        super(repository);
        this.factureMapper = factureMapper;
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
}
