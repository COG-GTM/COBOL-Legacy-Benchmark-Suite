package com.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Composite primary key for HistoryRecord (TRANSACTION_HISTORY_VSAM table).
 * Key: (PORTFOLIO_ID, TXN_DATE, SEQ) - replaces VSAM transaction history cluster key.
 */
public class HistoryRecordKey implements Serializable {

    private String portfolioId;
    private LocalDate txnDate;
    private int seq;

    public HistoryRecordKey() {}

    public HistoryRecordKey(String portfolioId, LocalDate txnDate, int seq) {
        this.portfolioId = portfolioId;
        this.txnDate = txnDate;
        this.seq = seq;
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }

    public LocalDate getTxnDate() { return txnDate; }
    public void setTxnDate(LocalDate txnDate) { this.txnDate = txnDate; }

    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryRecordKey that = (HistoryRecordKey) o;
        return seq == that.seq &&
               Objects.equals(portfolioId, that.portfolioId) &&
               Objects.equals(txnDate, that.txnDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, txnDate, seq);
    }
}
