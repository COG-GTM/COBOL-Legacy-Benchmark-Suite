package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** POSREC.cpy POS-KEY: POS-PORTFOLIO-ID X(8) + POS-DATE X(8) + POS-INVESTMENT-ID X(10). */
@Embeddable
public class PositionId implements Serializable {

    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;

    @Column(name = "INVESTMENT_ID", length = 10, nullable = false)
    private String investmentId;

    protected PositionId() {
    }

    public PositionId(String portfolioId, LocalDate positionDate, String investmentId) {
        this.portfolioId = portfolioId;
        this.positionDate = positionDate;
        this.investmentId = investmentId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public LocalDate getPositionDate() {
        return positionDate;
    }

    public String getInvestmentId() {
        return investmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionId that)) return false;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(positionDate, that.positionDate)
                && Objects.equals(investmentId, that.investmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, positionDate, investmentId);
    }

    @Override
    public String toString() {
        return portfolioId + "/" + positionDate + "/" + investmentId;
    }
}
