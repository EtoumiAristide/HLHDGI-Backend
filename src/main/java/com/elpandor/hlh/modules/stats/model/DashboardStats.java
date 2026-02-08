package com.elpandor.hlh.modules.stats.model;

import java.math.BigDecimal;

public interface DashboardStats {
    BigDecimal getTotalSale();
    BigDecimal getTotalPurchase();
    BigDecimal getTotalAvoir();
}
