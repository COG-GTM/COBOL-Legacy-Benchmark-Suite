package com.portfolio.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class HistoryKey implements Serializable {
  @Column(name = "PORTFOLIO_ID", columnDefinition = "char(8)")
  private String portfolioId;

  @Column(name = "HIST_DATE", columnDefinition = "char(8)")
  private String histDate;

  @Column(name = "HIST_TIME", columnDefinition = "char(6)")
  private String histTime;

  @Column(name = "SEQ_NO", columnDefinition = "char(4)")
  private String seqNo;

  public HistoryKey() {}

  public HistoryKey(String portfolioId, String histDate, String histTime, String seqNo) {
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

  public boolean equals(Object otherObject) {
    if (this == otherObject) return true;
    if (!(otherObject instanceof HistoryKey otherKey)) return false;
    return Objects.equals(portfolioId, otherKey.portfolioId)
        && Objects.equals(histDate, otherKey.histDate)
        && Objects.equals(histTime, otherKey.histTime)
        && Objects.equals(seqNo, otherKey.seqNo);
  }

  public int hashCode() {
    return Objects.hash(portfolioId, histDate, histTime, seqNo);
  }
}
