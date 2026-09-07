package com.portfolio.domain;

import com.portfolio.domain.converter.PositionStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "INVESTMENT_POSITIONS")
public class InvestmentPosition {
  @EmbeddedId private InvestmentPositionId id;

  @Column(precision = 18, scale = 4)
  private BigDecimal quantity;

  @Column(name = "COST_BASIS", precision = 18, scale = 2)
  private BigDecimal costBasis;

  @Column(name = "MARKET_VALUE", precision = 18, scale = 2)
  private BigDecimal marketValue;

  @Column(name = "CURRENCY_CODE", columnDefinition = "char(3)")
  private String currencyCode;

  @Convert(converter = PositionStatusConverter.class)
  @Column(columnDefinition = "char(1)")
  private PositionStatus status;

  @Column(name = "LAST_MAINT_DATE")
  private LocalDateTime lastMaintDate;

  @Column(name = "LAST_MAINT_USER", length = 8)
  private String lastMaintUser;

  public InvestmentPosition() {}

  public InvestmentPositionId getId() {
    return id;
  }

  public void setId(InvestmentPositionId id) {
    this.id = id;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getCostBasis() {
    return costBasis;
  }

  public void setCostBasis(BigDecimal costBasis) {
    this.costBasis = costBasis;
  }

  public BigDecimal getMarketValue() {
    return marketValue;
  }

  public void setMarketValue(BigDecimal marketValue) {
    this.marketValue = marketValue;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public PositionStatus getStatus() {
    return status;
  }

  public void setStatus(PositionStatus status) {
    this.status = status;
  }

  public LocalDateTime getLastMaintDate() {
    return lastMaintDate;
  }

  public void setLastMaintDate(LocalDateTime lastMaintDate) {
    this.lastMaintDate = lastMaintDate;
  }

  public String getLastMaintUser() {
    return lastMaintUser;
  }

  public void setLastMaintUser(String lastMaintUser) {
    this.lastMaintUser = lastMaintUser;
  }
}
