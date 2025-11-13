package com.elpandor.hlh.modules.hlh.model.dto.payload;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentSummary {
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private Payment.PaymentType pmtType;
    
    private Integer qty;
    
    private BigDecimal amount;
    
    private BigDecimal tip;
    
    private BigDecimal total;
    
    private BigDecimal percentTot;
}