package com.elpandor.hlh.modules.automatisationzino.infrastructure.parser;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@Component
public class TicketVenteZino {
    private Date date;
    private String nomClient;
    private String modePaiement;
    private Double montantHT;
    private Double tva;
    private Double montantTTC;
    private String codeProduitPrincipal;
    private String designationPrincipale;
    private Integer numTicket;
    private String numCompteClient;
}