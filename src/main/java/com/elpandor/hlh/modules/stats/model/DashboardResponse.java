package com.elpandor.hlh.modules.stats.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class DashboardResponse {

    private BigDecimal totalSale;
    private BigDecimal totalPurchase;
    private BigDecimal totalAvoir;
    private BigDecimal totalRevenue;

    private BigDecimal percentSale;
    private BigDecimal percentPurchase;
    private BigDecimal percentAvoir;

    private List<DashboardMonthly> monthly;

    public DashboardResponse(BigDecimal totalSale,
                             BigDecimal totalPurchase,
                             BigDecimal totalAvoir,
                             List<DashboardMonthly> monthly) {

        this.totalSale = nvl(totalSale);
        this.totalPurchase = nvl(totalPurchase);
        this.totalAvoir = nvl(totalAvoir);

        this.totalRevenue = this.totalSale
                        .add(this.totalPurchase)
                        .subtract(this.totalAvoir);

        BigDecimal totalForPercent = this.totalSale
                        .add(this.totalPurchase)
                        .add(this.totalAvoir);

        if (totalForPercent.compareTo(BigDecimal.ZERO) == 0) {
            this.percentSale = BigDecimal.ZERO;
            this.percentPurchase = BigDecimal.ZERO;
            this.percentAvoir = BigDecimal.ZERO;
        } else {
            this.percentSale = percent(this.totalSale, totalForPercent);
            this.percentPurchase = percent(this.totalPurchase, totalForPercent);
            this.percentAvoir = percent(this.totalAvoir, totalForPercent);
        }

        this.monthly = monthly;
    }

    // ---------- Helpers ----------
    private static BigDecimal percent(BigDecimal value, BigDecimal total) {
        return value
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ---------- Factory ----------
    public static DashboardResponse from(DashboardStats stats,
                                         List<DashboardMonthly> monthly) {

        return new DashboardResponse(
                stats.getTotalSale(),
                stats.getTotalPurchase(),
                stats.getTotalAvoir(),
                monthly
        );
    }

    // ---------- Getters ----------

    public BigDecimal getTotalSale() {
        return totalSale;
    }

    public BigDecimal getTotalPurchase() {
        return totalPurchase;
    }

    public BigDecimal getTotalAvoir() {
        return totalAvoir;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getPercentSale() {
        return percentSale;
    }

    public BigDecimal getPercentPurchase() {
        return percentPurchase;
    }

    public BigDecimal getPercentAvoir() {
        return percentAvoir;
    }

    public List<DashboardMonthly> getMonthly() {
        return monthly;
    }
}
