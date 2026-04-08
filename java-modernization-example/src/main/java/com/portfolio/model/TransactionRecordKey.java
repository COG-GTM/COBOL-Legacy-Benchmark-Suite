package com.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Composite key for TransactionRecord entity.
 * Maps the TRN-KEY group from TRNREC.cpy:
 * <pre>
 *     05  TRN-KEY.
 *         10  TRN-DATE           PIC X(08).
 *         10  TRN-TIME           PIC X(06).
 *         10  TRN-PORTFOLIO-ID   PIC X(08).
 *         10  TRN-SEQUENCE-NO    PIC X(06).
 * </pre>
 */
public class TransactionRecordKey implements Serializable {

    private LocalDate transDate;
    private LocalTime transTime;
    private String portfolioId;
    private String sequenceNo;

    public TransactionRecordKey() {
    }

    public TransactionRecordKey(LocalDate transDate, LocalTime transTime,
                                String portfolioId, String sequenceNo) {
        this.transDate = transDate;
        this.transTime = transTime;
        this.portfolioId = portfolioId;
        this.sequenceNo = sequenceNo;
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
        TransactionRecordKey that = (TransactionRecordKey) o;
        return Objects.equals(transDate, that.transDate)
                && Objects.equals(transTime, that.transTime)
                && Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(sequenceNo, that.sequenceNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transDate, transTime, portfolioId, sequenceNo);
    }
}
