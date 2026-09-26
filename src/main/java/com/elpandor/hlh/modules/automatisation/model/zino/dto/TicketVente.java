package com.elpandor.hlh.modules.automatisation.model.zino.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TicketVente {
    private String client;
    private String nomClient;
    private String prenomClient;
    private LocalDate date;
    private String codeClient;
    private String numeroTicket;
    private List<Detail> details = new ArrayList<>();
    private Paiement paiement;
}
