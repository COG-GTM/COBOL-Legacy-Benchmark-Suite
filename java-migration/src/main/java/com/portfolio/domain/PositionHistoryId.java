package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Embeddable
public class PositionHistoryId implements Serializable {
  @Column(name = "ACCOUNT_NO", columnDefinition = "char(10)")
  private String accountNo;

  @Column(name = "PORTFOLIO_ID", columnDefinition = "char(10)")
  private String portfolioId;

  private LocalDate transDate;
  private LocalTime transTime;

  public PositionHistoryId() {}

  public PositionHistoryId(
      String accountNo, String portfolioId, LocalDate transDate, LocalTime transTime) {
    this.accountNo = accountNo;
    this.portfolioId = portfolioId;
    this.transDate = transDate;
    this.transTime = transTime;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public void setAccountNo(String accountNo) {
    this.accountNo = accountNo;
  }

  public String getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(String portfolioId) {
    this.portfolioId = portfolioId;
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

  public boolean equals(Object otherObject) {
    if (this == otherObject) return true;
    if (!(otherObject instanceof PositionHistoryId otherId)) return false;
    return Objects.equals(accountNo, otherId.accountNo)
        && Objects.equals(portfolioId, otherId.portfolioId)
        && Objects.equals(transDate, otherId.transDate)
        && Objects.equals(transTime, otherId.transTime);
  }

  public int hashCode() {
    return Objects.hash(accountNo, portfolioId, transDate, transTime);
  }
}
