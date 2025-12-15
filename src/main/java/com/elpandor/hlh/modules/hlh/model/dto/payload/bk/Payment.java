package com.elpandor.hlh.modules.hlh.model.dto.payload.bk;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Payment {
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

    public enum PaymentType {
        CASH, BACKUP_CC, HD_GLOVO, CASH_WAVE
    }
}
