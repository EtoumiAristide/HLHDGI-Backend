package com.elpandor.hlh.modules.stats.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FactureTimbreRequest {
    private String numcc;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String pointDeVente;
}
