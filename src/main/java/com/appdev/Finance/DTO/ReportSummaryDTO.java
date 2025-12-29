package com.appdev.Finance.DTO;

import java.math.BigDecimal;

public class ReportSummaryDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netTotal;

    // Constructor
    public ReportSummaryDTO(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netTotal) {
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netTotal = netTotal;
    }

    // Getters (Setters usually not needed for simple data holders)
    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public BigDecimal getNetTotal() {
        return netTotal;
    }
}