package com.elpandor.hlh.modules.stats.service;

import com.elpandor.hlh.modules.stats.model.DashboardRequest;
import com.elpandor.hlh.modules.stats.model.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(DashboardRequest request);
}
