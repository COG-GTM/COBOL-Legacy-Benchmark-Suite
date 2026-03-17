package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite key for InvestmentPosition entity.
 * Migrated from: DB2 INVESTMENT_POSITIONS primary key (portfolio_id, investment_id, position_date)
 * and VSAM POSREC key structure (POS-KEY: Portfolio ID 8 + Date 8 + Investment ID 10).
 */
@Embeddable
public class InvestmentPositionKey implements Serializable {

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "investment_id", length = 10, nullable = false)
    private String investmentId;

    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    public InvestmentPositionKey() {
    }

    public InvestmentPositionKey(String portfolioId, String investmentId, LocalDate positionDate) {
        this.portfolioId = portfolioId;
        this.investmentId = investmentId;
        this.positionDate = positionDate;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(String investmentId) {
        this.investmentId = investmentId;
    }

    public LocalDate getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(LocalDate positionDate) {
        this.positionDate = positionDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvestmentPositionKey that = (InvestmentPositionKey) o;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(investmentId, that.investmentId)
                && Objects.equals(positionDate, that.positionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, investmentId, positionDate);
    }
}
