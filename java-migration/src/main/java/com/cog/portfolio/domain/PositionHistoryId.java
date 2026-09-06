package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** POSHIST.sql primary key (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME). */
@Embeddable
public class PositionHistoryId implements Serializable {

    @Column(name = "ACCOUNT_NO", length = 10, nullable = false)
    private String accountNo;

    @Column(name = "PORTFOLIO_ID", length = 10, nullable = false)
    private String portfolioId;

    @Column(name = "TRANS_DATE", nullable = false)
    private LocalDate transDate;

    @Column(name = "TRANS_TIME", nullable = false)
    private LocalTime transTime;

    protected PositionHistoryId() {
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

    public String getPortfolioId() {
        return portfolioId;
    }

    public LocalDate getTransDate() {
        return transDate;
    }

    public LocalTime getTransTime() {
        return transTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionHistoryId that)) return false;
        return Objects.equals(accountNo, that.accountNo)
                && Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(transDate, that.transDate)
                && Objects.equals(transTime, that.transTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNo, portfolioId, transDate, transTime);
    }

    @Override
    public String toString() {
        return accountNo + "/" + portfolioId + "/" + transDate + "T" + transTime;
    }
}
