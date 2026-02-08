package com.elpandor.hlh.modules.stats.model;

import lombok.Data;

@Data
public class DashboardRequest {
    private int annee;
    private long organisationId;
    private long etablissementId;
    private long pointVenteId;
    private String client;
}
