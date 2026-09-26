package com.elpandor.hlh.modules.automatisation.model.zino.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class Detail {
    private String reference;
    private String description;
    private BigDecimal quantite;
    private BigDecimal prixUnitaire;
    private Integer tauxTVA;
    private BigDecimal montantTotal;
    private Integer numTicket;
    private String codeClient;
    private LocalDate date;
}
