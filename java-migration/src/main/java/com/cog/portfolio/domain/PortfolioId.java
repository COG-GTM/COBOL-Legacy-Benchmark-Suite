package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** PORTFLIO.cpy PORT-KEY: PORT-ID PIC X(8) + PORT-ACCOUNT-NO PIC X(10). */
@Embeddable
public class PortfolioId implements Serializable {

    @Column(name = "PORT_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "ACCOUNT_NO", length = 10, nullable = false)
    private String accountNo;

    protected PortfolioId() {
    }

    public PortfolioId(String portfolioId, String accountNo) {
        this.portfolioId = portfolioId;
        this.accountNo = accountNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortfolioId that)) return false;
        return Objects.equals(portfolioId, that.portfolioId) && Objects.equals(accountNo, that.accountNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, accountNo);
    }

    @Override
    public String toString() {
        return portfolioId + "/" + accountNo;
    }
}
