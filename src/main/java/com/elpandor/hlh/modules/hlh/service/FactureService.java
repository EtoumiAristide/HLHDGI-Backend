package com.elpandor.hlh.modules.hlh.service;

import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FactureService extends GenericService<Facture, Integer, FactureDto> {
    public Page<FactureDto> findByEntreprise(Pageable pageable, String entreprise);
    public FactureDto findByNumFactureFNE(String numFactureFNE);
}
