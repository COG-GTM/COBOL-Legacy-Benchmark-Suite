package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Composite primary key for {@link HistoryRecord}, mirroring the VSAM KSDS
 * record key HIST-KEY = HIST-PORTFOLIO-ID + HIST-DATE + HIST-TIME + HIST-SEQ-NO (HISTREC.cpy).
 */
@Embeddable
public class HistoryRecordId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** HIST-PORTFOLIO-ID PIC X(08). */
    @Column(name = "PORTFOLIO_ID", columnDefinition = "CHAR(8)", length = 8, nullable = false)
    private String portfolioId;

    /** HIST-DATE PIC X(08) (YYYYMMDD). */
    @Column(name = "HIST_DATE", nullable = false)
    private LocalDate histDate;

    /** HIST-TIME PIC X(06) (HHMMSS). */
    @Column(name = "HIST_TIME", nullable = false)
    private LocalTime histTime;

    /** HIST-SEQ-NO PIC X(04) (zero-padded sequence kept as CHAR). */
    @Column(name = "SEQ_NO", columnDefinition = "CHAR(4)", length = 4, nullable = false)
    private String seqNo;

    public HistoryRecordId() {
    }

    public HistoryRecordId(String portfolioId, LocalDate histDate, LocalTime histTime, String seqNo) {
        this.portfolioId = portfolioId;
        this.histDate = histDate;
        this.histTime = histTime;
        this.seqNo = seqNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getHistDate() {
        return histDate;
    }

    public void setHistDate(LocalDate histDate) {
        this.histDate = histDate;
    }

    public LocalTime getHistTime() {
        return histTime;
    }

    public void setHistTime(LocalTime histTime) {
        this.histTime = histTime;
    }

    public String getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(String seqNo) {
        this.seqNo = seqNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistoryRecordId other)) {
            return false;
        }
        return Objects.equals(portfolioId, other.portfolioId)
                && Objects.equals(histDate, other.histDate)
                && Objects.equals(histTime, other.histTime)
                && Objects.equals(seqNo, other.seqNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, histDate, histTime, seqNo);
    }
}
