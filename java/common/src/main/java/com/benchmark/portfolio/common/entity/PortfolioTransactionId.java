package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Composite primary key for {@link PortfolioTransaction}, mirroring the VSAM KSDS
 * record key TRN-KEY = TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO (TRNREC.cpy).
 */
@Embeddable
public class PortfolioTransactionId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** TRN-DATE PIC X(08) (YYYYMMDD). */
    @Column(name = "TRANS_DATE", nullable = false)
    private LocalDate transDate;

    /** TRN-TIME PIC X(06) (HHMMSS). */
    @Column(name = "TRANS_TIME", nullable = false)
    private LocalTime transTime;

    /** TRN-PORTFOLIO-ID PIC X(08). */
    @Column(name = "PORTFOLIO_ID", columnDefinition = "CHAR(8)", length = 8, nullable = false)
    private String portfolioId;

    /** TRN-SEQUENCE-NO PIC X(06) (zero-padded sequence kept as CHAR). */
    @Column(name = "SEQUENCE_NO", columnDefinition = "CHAR(6)", length = 6, nullable = false)
    private String sequenceNo;

    public PortfolioTransactionId() {
    }

    public PortfolioTransactionId(LocalDate transDate, LocalTime transTime,
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortfolioTransactionId other)) {
            return false;
        }
        return Objects.equals(transDate, other.transDate)
                && Objects.equals(transTime, other.transTime)
                && Objects.equals(portfolioId, other.portfolioId)
                && Objects.equals(sequenceNo, other.sequenceNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transDate, transTime, portfolioId, sequenceNo);
    }
}
