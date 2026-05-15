package com.elpandor.hlh.modules.stats.service;

import com.elpandor.hlh.modules.stats.model.FactureTimbre;
import com.elpandor.hlh.modules.stats.model.FactureTimbreRequest;
import com.elpandor.hlh.modules.stats.model.FactureTimbreTotaux;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.util.Map;

public interface RapportStatsService {
    Page<FactureTimbre> getFactureTimbre(int page, int size, String sortBy, String direction, FactureTimbreRequest request);

    public byte[] exportTimbreToExcel(FactureTimbreRequest request) throws IOException;

    public Map<String, Map<String, Map<String, FactureTimbreTotaux>>> getFactureTimbreTotaux(FactureTimbreRequest request);
}
