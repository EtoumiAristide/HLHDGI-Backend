package com.elpandor.hlh.modules.stats.rest;

import com.elpandor.hlh.modules.stats.model.DashboardRequest;
import com.elpandor.hlh.modules.stats.model.DashboardResponse;
import com.elpandor.hlh.modules.stats.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardApi {

    private final DashboardService dashboardService;

    @PostMapping
    public DashboardResponse getDashboard(@RequestBody DashboardRequest request) {
        return dashboardService.getDashboard(request);
    }
}
