package com.elpandor.hlh.modules.bk.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BkTimbreMonthlyReportDto {
    private String period;
    private BigDecimal totalStampDuty;
    private BigDecimal threshold;
    private BigDecimal fixedStampDuty;
    private long totalTransactions;
    private long eligibleTransactions;
    private List<BkTimbreDetailDto> details;
}
