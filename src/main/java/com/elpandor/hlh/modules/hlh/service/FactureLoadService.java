package com.elpandor.hlh.modules.hlh.service;

import com.elpandor.hlh.common.core.base.GenericService;
import com.elpandor.hlh.modules.hlh.model.Facture;
import com.elpandor.hlh.modules.hlh.model.FactureLoad;
import com.elpandor.hlh.modules.hlh.model.dto.FactureDto;
import com.elpandor.hlh.modules.hlh.model.dto.FactureLoadDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FactureLoadService extends GenericService<FactureLoad, UUID, FactureLoadDto> {
    public Page<FactureLoadDto> findByEntreprise(Pageable pageable, String entreprise);
}
