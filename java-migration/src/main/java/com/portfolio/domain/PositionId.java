package com.portfolio.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for Position entity.
 * Maps VSAM KSDS key: PORTFOLIO-ID + POSITION-DATE + INVESTMENT-ID.
 */
public class PositionId implements Serializable {

    private String portfolioId;
    private String investmentId;
    private LocalDate positionDate;

    public PositionId() {}

    public PositionId(String portfolioId, String investmentId, LocalDate positionDate) {
        this.portfolioId = portfolioId;
        this.investmentId = investmentId;
        this.positionDate = positionDate;
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public LocalDate getPositionDate() { return positionDate; }
    public void setPositionDate(LocalDate positionDate) { this.positionDate = positionDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionId that = (PositionId) o;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(investmentId, that.investmentId)
                && Objects.equals(positionDate, that.positionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, investmentId, positionDate);
    }
}
