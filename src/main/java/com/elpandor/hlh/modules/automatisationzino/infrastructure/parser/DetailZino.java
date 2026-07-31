package com.elpandor.hlh.modules.automatisationzino.infrastructure.parser;

import lombok.Data;

import java.util.Date;

@Data
public class DetailZino {
    private Date date;
    private String codeProduit;
    private String designation;
    private Integer numTicket;
    private String numCompteClient;
    private String reference;
    private double quantite;
    private double prixUnitaire;
    private double tauxTVA;
    private double montantHT;
    private double tva;
}
