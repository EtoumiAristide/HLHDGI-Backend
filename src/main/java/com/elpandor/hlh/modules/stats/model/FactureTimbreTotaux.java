package com.elpandor.hlh.modules.stats.model;

import java.math.BigDecimal;

public interface FactureTimbreTotaux{
    String getMois();
    String getMoyenDePaiement();
    Long getNombreFactures();
    Long getTotalTickets();
    BigDecimal getTotalMontant();
    String getPointDeVente();
}
