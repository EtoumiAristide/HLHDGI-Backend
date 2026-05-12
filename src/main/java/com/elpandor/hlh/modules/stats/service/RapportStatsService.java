package com.elpandor.hlh.modules.stats.service;

import com.elpandor.hlh.modules.stats.model.FactureTimbre;
import com.elpandor.hlh.modules.stats.model.FactureTimbreRequest;
import org.springframework.data.domain.Page;

import java.io.IOException;

public interface RapportStatsService {
    Page<FactureTimbre> getFactureTimbre(int page, int size, String sortBy, String direction, FactureTimbreRequest request);

    public byte[] exportTimbreToExcel(FactureTimbreRequest request) throws IOException;
}
