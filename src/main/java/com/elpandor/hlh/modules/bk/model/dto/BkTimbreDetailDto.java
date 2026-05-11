package com.elpandor.hlh.modules.bk.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BkTimbreDetailDto {
    private String transactionId;
    private String checkNumber;
    private String paymentType;
    private BigDecimal amount;
    private BigDecimal stampDuty;
    private boolean eligible;
    private String eligibilityReason;
    private String period;
}
