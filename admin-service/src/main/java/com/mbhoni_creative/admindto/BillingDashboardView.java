package com.mbhoni_creative.admindto;

import java.math.BigDecimal;

public class BillingDashboardView {

    private BigDecimal totalInvoiced = BigDecimal.ZERO;
    private BigDecimal totalPaid = BigDecimal.ZERO;
    private BigDecimal outstandingBalance = BigDecimal.ZERO;
    private BigDecimal overdueBalance = BigDecimal.ZERO;

    private long totalInvoices;
    private long paidInvoices;
    private long overdueInvoices;
    private long issuedInvoices;
    private long partiallyPaidInvoices;

    public BigDecimal getTotalInvoiced() {
        return totalInvoiced;
    }

    public void setTotalInvoiced(BigDecimal totalInvoiced) {
        this.totalInvoiced = totalInvoiced;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public BigDecimal getOverdueBalance() {
        return overdueBalance;
    }

    public void setOverdueBalance(BigDecimal overdueBalance) {
        this.overdueBalance = overdueBalance;
    }

    public long getTotalInvoices() {
        return totalInvoices;
    }

    public void setTotalInvoices(long totalInvoices) {
        this.totalInvoices = totalInvoices;
    }

    public long getPaidInvoices() {
        return paidInvoices;
    }

    public void setPaidInvoices(long paidInvoices) {
        this.paidInvoices = paidInvoices;
    }

    public long getOverdueInvoices() {
        return overdueInvoices;
    }

    public void setOverdueInvoices(long overdueInvoices) {
        this.overdueInvoices = overdueInvoices;
    }

    public long getIssuedInvoices() {
        return issuedInvoices;
    }

    public void setIssuedInvoices(long issuedInvoices) {
        this.issuedInvoices = issuedInvoices;
    }

    public long getPartiallyPaidInvoices() {
        return partiallyPaidInvoices;
    }

    public void setPartiallyPaidInvoices(long partiallyPaidInvoices) {
        this.partiallyPaidInvoices = partiallyPaidInvoices;
    }
}