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
public class HistoryRecordKey implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 8)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "portfolio_id", length = 8, nullable = false, columnDefinition = "CHAR(8)")
    private String portfolioId;

    @NotNull
    @Column(name = "history_date", nullable = false)
    private LocalDate historyDate;

    @NotNull
    @Column(name = "history_time", nullable = false)
    private LocalTime historyTime;

    @NotNull
    @Size(max = 4)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sequence_no", length = 4, nullable = false, columnDefinition = "CHAR(4)")
    private String sequenceNo;

    protected HistoryRecordKey() {
    }

    public HistoryRecordKey(String portfolioId, LocalDate historyDate,
                            LocalTime historyTime, String sequenceNo) {
        this.portfolioId = portfolioId;
        this.historyDate = historyDate;
        this.historyTime = historyTime;
        this.sequenceNo = sequenceNo;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public LocalDate getHistoryDate() {
        return historyDate;
    }

    public void setHistoryDate(LocalDate historyDate) {
        this.historyDate = historyDate;
    }

    public LocalTime getHistoryTime() {
        return historyTime;
    }

    public void setHistoryTime(LocalTime historyTime) {
        this.historyTime = historyTime;
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
        if (!(o instanceof HistoryRecordKey that)) {
            return false;
        }
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
