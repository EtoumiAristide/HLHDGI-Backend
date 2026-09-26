package com.elpandor.hlh.modules.stats.service.impl;

import com.elpandor.hlh.modules.hlh.repository.FactureRepository;
import com.elpandor.hlh.modules.stats.model.DashboardMonthly;
import com.elpandor.hlh.modules.stats.model.DashboardRequest;
import com.elpandor.hlh.modules.stats.model.DashboardResponse;
import com.elpandor.hlh.modules.stats.model.DashboardStats;
import com.elpandor.hlh.modules.stats.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final FactureRepository factureRepository;

    @Override
    public DashboardResponse getDashboard(DashboardRequest request) {

        validate(request);

        DashboardStats totals = factureRepository.getDashboardStats(
                request.getAnnee(),
                request.getOrganisationId(),
                request.getEtablissementId(),
                request.getPointVenteId(),
                normalizeClient(request.getClient())
        );

        List<DashboardMonthly> monthly = factureRepository.getMonthlyStats(
                        request.getAnnee(),
                        request.getOrganisationId(),
                        request.getEtablissementId(),
                        request.getPointVenteId(),
                        normalizeClient(request.getClient())
                );

        return DashboardResponse.from(totals, monthly);
    }

    // ---------- Validation métier ----------

    private void validate(DashboardRequest request) {

        if (request.getAnnee() <= 0) {
            throw new IllegalArgumentException("L'année est obligatoire");
        }

        // Normalisation des paramètres
        long normalizedOrgId = (request.getOrganisationId() > 0) ? request.getOrganisationId() : 0;
        long normalizedEtabId = (request.getEtablissementId() > 0) ? request.getEtablissementId() : 0;
        long normalizedPdvId = (request.getPointVenteId() > 0) ? request.getPointVenteId() : 0;
        String normalizedClient = StringUtils.hasText(request.getClient()) ? request.getClient().trim() : null;

        // Logique de validation supplémentaire
        if (normalizedEtabId > 0 && normalizedOrgId == 0) {
            throw new IllegalArgumentException("L'ID établissement ne peut être utilisé sans ID organisation");
        }

        if (normalizedPdvId > 0 && normalizedEtabId == 0) {
            throw new IllegalArgumentException("L'ID point de vente ne peut être utilisé sans ID établissement");
        }
    }

    // ---------- Helpers ----------

    private String normalizeClient(String client) {
        return StringUtils.hasText(client) ? client.trim() : null;
    }
}

