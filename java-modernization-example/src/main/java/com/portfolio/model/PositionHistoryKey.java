package com.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Composite key for PositionHistory entity.
 * Maps the POSHIST primary key: (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
 */
public class PositionHistoryKey implements Serializable {

    private String accountNo;
    private String portfolioId;
    private LocalDate transDate;
    private LocalTime transTime;

    public PositionHistoryKey() {
    }

    public PositionHistoryKey(String accountNo, String portfolioId,
                              LocalDate transDate, LocalTime transTime) {
        this.accountNo = accountNo;
        this.portfolioId = portfolioId;
        this.transDate = transDate;
        this.transTime = transTime;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getTransDate() {
        return transDate;
    }

    public void setTransDate(LocalDate transDate) {
        this.transDate = transDate;
    }

    public LocalTime getTransTime() {
        return transTime;
    }

    public void setTransTime(LocalTime transTime) {
        this.transTime = transTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionHistoryKey that = (PositionHistoryKey) o;
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
