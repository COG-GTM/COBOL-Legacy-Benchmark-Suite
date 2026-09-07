package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class InvestmentPositionId implements Serializable {
  @Column(name = "PORTFOLIO_ID", columnDefinition = "char(8)")
  private String portfolioId;

  @Column(name = "INVESTMENT_ID", columnDefinition = "char(10)")
  private String investmentId;

  @Column(name = "POSITION_DATE")
  private LocalDate positionDate;

  public InvestmentPositionId() {}

  public InvestmentPositionId(String portfolioId, String investmentId, LocalDate positionDate) {
    this.portfolioId = portfolioId;
    this.investmentId = investmentId;
    this.positionDate = positionDate;
  }

  public String getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(String portfolioId) {
    this.portfolioId = portfolioId;
  }

  public String getInvestmentId() {
    return investmentId;
  }

  public void setInvestmentId(String investmentId) {
    this.investmentId = investmentId;
  }

  public LocalDate getPositionDate() {
    return positionDate;
  }

  public void setPositionDate(LocalDate positionDate) {
    this.positionDate = positionDate;
  }

  public boolean equals(Object otherObject) {
    if (this == otherObject) return true;
    if (!(otherObject instanceof InvestmentPositionId otherId)) return false;
    return Objects.equals(portfolioId, otherId.portfolioId)
        && Objects.equals(investmentId, otherId.investmentId)
        && Objects.equals(positionDate, otherId.positionDate);
  }

  public int hashCode() {
    return Objects.hash(portfolioId, investmentId, positionDate);
  }
}
