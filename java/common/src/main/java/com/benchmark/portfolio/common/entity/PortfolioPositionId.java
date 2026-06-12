package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for {@link PortfolioPosition}, mirroring the VSAM KSDS
 * record key POS-KEY = POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID (POSREC.cpy).
 */
@Embeddable
public class PortfolioPositionId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** POS-PORTFOLIO-ID PIC X(08). */
    @Column(name = "PORTFOLIO_ID", columnDefinition = "CHAR(8)", length = 8, nullable = false)
    private String portfolioId;

    /** POS-DATE PIC X(08) (YYYYMMDD). */
    @Column(name = "POSITION_DATE", nullable = false)
    private LocalDate positionDate;

    /** POS-INVESTMENT-ID PIC X(10). */
    @Column(name = "INVESTMENT_ID", columnDefinition = "CHAR(10)", length = 10, nullable = false)
    private String investmentId;

    public PortfolioPositionId() {
    }

    public PortfolioPositionId(String portfolioId, LocalDate positionDate, String investmentId) {
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
        if (!(o instanceof PortfolioPositionId other)) {
            return false;
        }
        return Objects.equals(portfolioId, other.portfolioId)
                && Objects.equals(positionDate, other.positionDate)
                && Objects.equals(investmentId, other.investmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, positionDate, investmentId);
    }
}
