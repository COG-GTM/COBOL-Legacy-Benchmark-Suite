package com.cog.gtm.clbs.migration.domain.transaction;

import java.io.Serializable;
import java.util.Objects;

public class TransactionId implements Serializable {

    private String transactionDate;
    private String transactionTime;
    private String portfolioId;
    private String sequenceNo;

    public TransactionId() {
    }

    public TransactionId(String transactionDate, String transactionTime, String portfolioId, String sequenceNo) {
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.portfolioId = portfolioId;
        this.sequenceNo = sequenceNo;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return Objects.equals(transactionDate, that.transactionDate)
                && Objects.equals(transactionTime, that.transactionTime)
                && Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(sequenceNo, that.sequenceNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionDate, transactionTime, portfolioId, sequenceNo);
    }
}
