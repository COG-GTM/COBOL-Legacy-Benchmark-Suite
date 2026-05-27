package com.portfolio.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class HistoryRecordId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "portfolio_id", length = 8)
    private String portfolioId;

    @Column(name = "hist_date", length = 8)
    private String histDate;

    @Column(name = "hist_time", length = 6)
    private String histTime;

    @Column(name = "seq_no", length = 4)
    private String seqNo;

    public HistoryRecordId() {
    }

    public HistoryRecordId(String portfolioId, String histDate, String histTime, String seqNo) {
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

    public String getHistDate() {
        return histDate;
    }

    public void setHistDate(String histDate) {
        this.histDate = histDate;
    }

    public String getHistTime() {
        return histTime;
    }

    public void setHistTime(String histTime) {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryRecordId that = (HistoryRecordId) o;
        return Objects.equals(portfolioId, that.portfolioId)
                && Objects.equals(histDate, that.histDate)
                && Objects.equals(histTime, that.histTime)
                && Objects.equals(seqNo, that.seqNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId, histDate, histTime, seqNo);
    }
}
