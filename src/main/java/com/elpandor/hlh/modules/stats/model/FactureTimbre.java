package com.elpandor.hlh.modules.stats.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FactureTimbre {
    String getMois();
    String getBkName();
    String getNFacture();
    LocalDate getJourCa();
    String getMoyenDePaiement();
    Integer getNbreTicket5000();
    Integer getMontantTimbre();
    BigDecimal getTotal();
}
