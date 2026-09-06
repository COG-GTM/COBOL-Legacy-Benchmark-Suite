package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** HISTREC.cpy HIST-KEY: HIST-PORTFOLIO-ID X(8) + HIST-DATE X(8) + HIST-TIME X(6) + HIST-SEQ-NO X(4). */
@Embeddable
public class HistoryId implements Serializable {

    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "HIST_DATE", nullable = false)
    private LocalDate historyDate;

    @Column(name = "HIST_TIME", nullable = false)
    private LocalTime historyTime;

    @Column(name = "SEQ_NO", length = 4, nullable = false)
    private String sequenceNo;

    protected HistoryId() {
    }

    public HistoryId(String portfolioId, LocalDate historyDate, LocalTime historyTime, String sequenceNo) {
        this.portfolioId = portfolioId;
        this.historyDate = historyDate;
        this.historyTime = historyTime;
        this.sequenceNo = sequenceNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public LocalDate getHistoryDate() {
        return historyDate;
    }

    public LocalTime getHistoryTime() {
        return historyTime;
    }

    public String getSequenceNo() {
        return sequenceNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoryId that)) return false;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(historyDate, that.historyDate)
                && Objects.equals(historyTime, that.historyTime)
                && Objects.equals(sequenceNo, that.sequenceNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, historyDate, historyTime, sequenceNo);
    }
}
