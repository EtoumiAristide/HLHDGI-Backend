package com.elpandor.hlh.modules.automatisation.model.zino.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class Paiement {
    private String codePaiement;
    private Integer quantite;
    private String moyenPaiement;
    private BigDecimal montant;
    private Integer numTicket;
    private String codeClient;
    private LocalDate date;
}
