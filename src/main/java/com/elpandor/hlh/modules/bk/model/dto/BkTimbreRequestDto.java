package com.elpandor.hlh.modules.bk.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BkTimbreRequestDto {
    private String period;
    private List<BkPaymentDto> payments;
}
