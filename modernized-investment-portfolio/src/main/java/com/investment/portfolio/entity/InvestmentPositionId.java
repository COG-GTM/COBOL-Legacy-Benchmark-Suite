package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for the InvestmentPosition entity.
 *
 * COBOL Source: POSREC.cpy (POS-KEY)
 *   POS-PORTFOLIO-ID PIC X(08)
 *   POS-DATE         PIC X(08)
 *   POS-INVESTMENT-ID PIC X(10)
 *
 * DB2 Source: db2-definitions.sql
 *   PRIMARY KEY (PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE)
 *
 * This matches the VSAM Position Master key structure (ACCOUNT-NO + FUND-ID)
 * combined with the DB2 date dimension for position tracking.
 */
@Embeddable
public class InvestmentPositionId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "portfolio_id", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String portfolioId;

    @Column(name = "investment_id", length = 10, nullable = false)
    @NotNull
    @Size(max = 10)
    private String investmentId;

    @Column(name = "position_date", nullable = false)
    @NotNull
    private LocalDate positionDate;

    public InvestmentPositionId() {
    }

    public InvestmentPositionId(String portfolioId, String investmentId, LocalDate positionDate) {
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
        InvestmentPositionId that = (InvestmentPositionId) o;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(investmentId, that.investmentId)
                && Objects.equals(positionDate, that.positionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, investmentId, positionDate);
    }
}
