package com.portfolio.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for PosHistRecord entity.
 * Maps DB2 PK: ACCOUNT_NO + PORTFOLIO_ID + TRANS_DATE + TRANS_TIME.
 */
public class PosHistId implements Serializable {

    private String accountNo;
    private String portfolioId;
    private LocalDate transDate;
    private String transTime;

    public PosHistId() {}

    public PosHistId(String accountNo, String portfolioId, LocalDate transDate, String transTime) {
        this.accountNo = accountNo;
        this.portfolioId = portfolioId;
        this.transDate = transDate;
        this.transTime = transTime;
    }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public LocalDate getTransDate() { return transDate; }
    public void setTransDate(LocalDate transDate) { this.transDate = transDate; }
    public String getTransTime() { return transTime; }
    public void setTransTime(String transTime) { this.transTime = transTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PosHistId that = (PosHistId) o;
        return Objects.equals(accountNo, that.accountNo)
                && Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(transDate, that.transDate)
                && Objects.equals(transTime, that.transTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNo, portfolioId, transDate, transTime);
    }
}
