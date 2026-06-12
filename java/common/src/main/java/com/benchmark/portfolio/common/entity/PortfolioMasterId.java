package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link PortfolioMaster}, mirroring the VSAM KSDS
 * record key PORT-KEY = PORT-ID + PORT-ACCOUNT-NO (PORTFLIO.cpy).
 */
@Embeddable
public class PortfolioMasterId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** PORT-ID PIC X(8). */
    @Column(name = "PORTFOLIO_ID", columnDefinition = "CHAR(8)", length = 8, nullable = false)
    private String portfolioId;

    /** PORT-ACCOUNT-NO PIC X(10). */
    @Column(name = "ACCOUNT_NO", columnDefinition = "CHAR(10)", length = 10, nullable = false)
    private String accountNo;

    public PortfolioMasterId() {
    }

    public PortfolioMasterId(String portfolioId, String accountNo) {
        this.portfolioId = portfolioId;
        this.accountNo = accountNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortfolioMasterId other)) {
            return false;
        }
        return Objects.equals(portfolioId, other.portfolioId)
                && Objects.equals(accountNo, other.accountNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, accountNo);
    }
}
