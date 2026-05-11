package com.elpandor.hlh.modules.bk.model.dto;

import com.elpandor.hlh.modules.hlh.model.dto.payload.bk.Payment.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BkPaymentDto {
    private Long id;
    private String checkNumber;
    private String cardNumber;
    private String exp;
    private Integer qty;
    private BigDecimal amount;
    private BigDecimal tip;
    private BigDecimal tdt;
    private BigDecimal tva;
    private BigDecimal total;
    private String emp;
    private PaymentType paymentType;
}
