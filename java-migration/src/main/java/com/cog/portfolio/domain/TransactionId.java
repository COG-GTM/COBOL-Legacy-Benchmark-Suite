package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** TRNREC.cpy TRN-KEY: TRN-DATE X(8) + TRN-TIME X(6) + TRN-PORTFOLIO-ID X(8) + TRN-SEQUENCE-NO X(6). */
@Embeddable
public class TransactionId implements Serializable {

    @Column(name = "TRN_DATE", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "TRN_TIME", nullable = false)
    private LocalTime transactionTime;

    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "SEQUENCE_NO", length = 6, nullable = false)
    private String sequenceNo;

    protected TransactionId() {
    }

    public TransactionId(LocalDate transactionDate, LocalTime transactionTime, String portfolioId, String sequenceNo) {
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.portfolioId = portfolioId;
        this.sequenceNo = sequenceNo;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public LocalTime getTransactionTime() {
        return transactionTime;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionId that)) return false;
        return Objects.equals(transactionDate, that.transactionDate)
                && Objects.equals(transactionTime, that.transactionTime)
                && Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(sequenceNo, that.sequenceNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionDate, transactionTime, portfolioId, sequenceNo);
    }

    @Override
    public String toString() {
        return transactionDate + "T" + transactionTime + "/" + portfolioId + "/" + sequenceNo;
    }
}
