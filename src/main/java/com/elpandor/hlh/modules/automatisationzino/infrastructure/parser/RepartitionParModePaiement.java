package com.elpandor.hlh.modules.automatisationzino.infrastructure.parser;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@Component
public class RepartitionParModePaiement {
    private Date date;
    private String modePaiement;
    private Double totalMontantHT;
    private Double totalTVA;
    private Double totalMontantTTC;
    private int nombreTransactions;
    private String designationPrincipale;
}