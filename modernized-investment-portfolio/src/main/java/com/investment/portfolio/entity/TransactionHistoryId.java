package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Composite primary key for the TransactionHistory (POSHIST) entity.
 *
 * DB2 Source: POSHIST.sql
 *   PRIMARY KEY (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
 *
 * COBOL Source: HISTREC.cpy (HIST-KEY)
 *   HIST-PORTFOLIO-ID PIC X(08)
 *   HIST-DATE         PIC X(08)
 *   HIST-TIME         PIC X(06)
 *   HIST-SEQ-NO       PIC X(04)
 */
@Embeddable
public class TransactionHistoryId implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Account Number.
     * DB2: ACCOUNT_NO CHAR(8) NOT NULL
     */
    @Column(name = "account_no", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String accountNo;

    /**
     * Portfolio Identifier.
     * COBOL: HIST-PORTFOLIO-ID PIC X(08)
     * DB2: PORTFOLIO_ID CHAR(10) NOT NULL
     */
    @Column(name = "portfolio_id", length = 10, nullable = false)
    @NotNull
    @Size(max = 10)
    private String portfolioId;

    /**
     * Transaction Date.
     * COBOL: HIST-DATE PIC X(08)
     * DB2: TRANS_DATE DATE NOT NULL
     */
    @Column(name = "trans_date", nullable = false)
    @NotNull
    private LocalDate transDate;

    /**
     * Transaction Time.
     * COBOL: HIST-TIME PIC X(06)
     * DB2: TRANS_TIME TIME NOT NULL
     */
    @Column(name = "trans_time", nullable = false)
    @NotNull
    private LocalTime transTime;

    public TransactionHistoryId() {
    }

    public TransactionHistoryId(String accountNo, String portfolioId, LocalDate transDate, LocalTime transTime) {
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
        TransactionHistoryId that = (TransactionHistoryId) o;
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
