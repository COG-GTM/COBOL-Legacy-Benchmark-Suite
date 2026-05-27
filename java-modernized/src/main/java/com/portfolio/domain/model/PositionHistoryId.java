package com.portfolio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Embeddable
public class PositionHistoryId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "account_no", length = 8)
    private String accountNo;

    @Column(name = "portfolio_id", length = 10)
    private String portfolioId;

    @Column(name = "trans_date")
    private LocalDate transDate;

    @Column(name = "trans_time")
    private LocalTime transTime;

    public PositionHistoryId() {
    }

    public PositionHistoryId(String accountNo, String portfolioId, LocalDate transDate, LocalTime transTime) {
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
        PositionHistoryId that = (PositionHistoryId) o;
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
