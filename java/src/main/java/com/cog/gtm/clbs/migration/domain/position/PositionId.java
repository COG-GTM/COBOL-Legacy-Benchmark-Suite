package com.cog.gtm.clbs.migration.domain.position;

import java.io.Serializable;
import java.util.Objects;

public class PositionId implements Serializable {

    private String portfolioId;
    private String positionDate;
    private String investmentId;

    public PositionId() {
    }

    public PositionId(String portfolioId, String positionDate, String investmentId) {
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

    public String getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(String positionDate) {
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
        PositionId that = (PositionId) o;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(positionDate, that.positionDate)
                && Objects.equals(investmentId, that.investmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, positionDate, investmentId);
    }
}
