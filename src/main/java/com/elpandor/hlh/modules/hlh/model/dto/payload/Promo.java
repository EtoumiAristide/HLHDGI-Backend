package com.elpandor.hlh.modules.hlh.model.dto.payload;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Promo {
    private Long id;
    
    private String checkNumber;
    
    private String name;
    
    private Integer qty;
    
    private BigDecimal amount;
    
    private BigDecimal percentTot;
    
    private String emp;
    
    private String mgr;
    
    private PromoType promoType;
    
    public enum PromoType {
        WHOPPER_MN, DBL_WHOPPER_MN, X_LONG_CHILI_CHS_MN, LONG_CHICKEN_MN
    }
}