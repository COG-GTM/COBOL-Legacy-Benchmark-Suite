package com.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Composite key for PositionHistory entity.
 * Migrated from: VSAM POSHIST key structure and HISTREC.cpy HIST-KEY.
 */
public class PositionHistoryKey implements Serializable {

    private String portfolioId;
    private LocalDate historyDate;
    private LocalTime historyTime;
    private String sequenceNo;

    public PositionHistoryKey() {
    }

    public PositionHistoryKey(String portfolioId, LocalDate historyDate,
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionHistoryKey that = (PositionHistoryKey) o;
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
