package com.elpandor.hlh.modules.automatisationzino.infrastructure.parser;

import com.elpandor.hlh.modules.automatisation.model.zino.dto.Detail;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

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
    private List<DetailZino> details;
}