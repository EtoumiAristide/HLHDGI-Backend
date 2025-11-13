package com.elpandor.hlh.modules.hlh.model.dto.payload;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Comp {
    private Long id;

    private String chkNumber;

    private String time;

    private String nameItem;

    private String unit;

    private Integer qty;

    private BigDecimal amount;

    private BigDecimal percentTot;

    private String emp;

    private String mgr;

    private CompType compType;

    public enum CompType {
        STAFF_MEALS, MANAGER_MEALS, GUEST_TRACK
    }
}