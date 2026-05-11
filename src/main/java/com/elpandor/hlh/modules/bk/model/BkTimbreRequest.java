package com.elpandor.hlh.modules.bk.model;

import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.Payment;
import lombok.Data;

import java.util.List;

@Data
public class BkTimbreRequest {
    private List<Payment> payments;
    private String period;
}
