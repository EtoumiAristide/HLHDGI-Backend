package com.elpandor.hlh.modules.stats.model;

import java.math.BigDecimal;

public interface DashboardMonthly {
    Integer getMonth();
    BigDecimal getSale();
    BigDecimal getPurchase();
    BigDecimal getAvoir();
}
