package com.elpandor.hlh.modules.bk.service;

import com.elpandor.hlh.modules.bk.model.BkTimbreDetail;
import com.elpandor.hlh.modules.bk.model.BkTimbreMonthlyReport;
import com.elpandor.hlh.modules.bk.model.BkTimbreRequest;

import java.util.List;

public interface BurgerKingTimbreService {
    List<BkTimbreDetail> calculateDetails(BkTimbreRequest request);

    BkTimbreMonthlyReport buildReport(BkTimbreRequest request);

    String exportCsv(List<BkTimbreDetail> details);

    byte[] exportExcel(List<BkTimbreDetail> details);

    // Nouveaux exports agrégés conformes au template client
    String exportAggregatedCsv(BkTimbreRequest request);

    byte[] exportAggregatedExcel(BkTimbreRequest request);
}
