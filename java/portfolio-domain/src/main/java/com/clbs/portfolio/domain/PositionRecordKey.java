package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class PositionRecordKey implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 8)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "portfolio_id", length = 8, nullable = false, columnDefinition = "CHAR(8)")
    private String portfolioId;

    @NotNull
    @Column(name = "position_date", nullable = false)
    private LocalDate positionDate;

    @NotNull
    @Size(max = 10)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "investment_id", length = 10, nullable = false, columnDefinition = "CHAR(10)")
    private String investmentId;

    protected PositionRecordKey() {
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof PositionRecordKey that)) {
            return false;
        }
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(positionDate, that.positionDate)
                && Objects.equals(investmentId, that.investmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, positionDate, investmentId);
    }
}
