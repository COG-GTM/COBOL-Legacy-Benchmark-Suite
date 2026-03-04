package com.cobolbenchmark.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite key for PositionRecord - from POSREC.cpy composite key
 * (POS-PORTFOLIO-ID, POS-DATE, POS-INVESTMENT-ID).
 */
public class PositionRecordKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String portfolioId;
    private LocalDate positionDate;
    private String investmentId;

    public PositionRecordKey() {
    }

    public PositionRecordKey(String portfolioId, LocalDate positionDate, String investmentId) {
        this.portfolioId = portfolioId;
        this.positionDate = positionDate;
        this.investmentId = investmentId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(LocalDate positionDate) {
        this.positionDate = positionDate;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionRecordKey that = (PositionRecordKey) o;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(positionDate, that.positionDate)
                && Objects.equals(investmentId, that.investmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, positionDate, investmentId);
    }
}
