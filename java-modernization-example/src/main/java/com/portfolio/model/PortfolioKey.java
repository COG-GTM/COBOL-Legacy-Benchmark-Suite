package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for Portfolio entity.
 * Maps the PORT-KEY group from PORTFLIO.cpy:
 * <pre>
 *     05  PORT-KEY.
 *         10  PORT-ID             PIC X(8).
 *         10  PORT-ACCOUNT-NO     PIC X(10).
 * </pre>
 */
@Embeddable
public class PortfolioKey implements Serializable {

    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNo;

    public PortfolioKey() {
    }

    public PortfolioKey(String portfolioId, String accountNo) {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioKey that = (PortfolioKey) o;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(accountNo, that.accountNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, accountNo);
    }
}
