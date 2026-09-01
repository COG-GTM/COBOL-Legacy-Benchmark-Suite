package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Embeddable
public class TransactionRecordKey implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 8)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "portfolio_id", length = 8, nullable = false, columnDefinition = "CHAR(8)")
    private String portfolioId;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotNull
    @Column(name = "transaction_time", nullable = false)
    private LocalTime transactionTime;

    @NotNull
    @Size(max = 6)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sequence_no", length = 6, nullable = false, columnDefinition = "CHAR(6)")
    private String sequenceNo;

    protected TransactionRecordKey() {
    }

    public TransactionRecordKey(String portfolioId, LocalDate transactionDate,
                                LocalTime transactionTime, String sequenceNo) {
        this.portfolioId = portfolioId;
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.sequenceNo = sequenceNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransactionRecordKey that)) {
            return false;
        }
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(transactionDate, that.transactionDate)
                && Objects.equals(transactionTime, that.transactionTime)
                && Objects.equals(sequenceNo, that.sequenceNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, transactionDate, transactionTime, sequenceNo);
    }
}
