package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DB2 {@code INVESTMENT_POSITIONS} (V1). */
@Entity
@Table(name = "INVESTMENT_POSITIONS")
public class InvestmentPosition {

  @EmbeddedId private InvestmentPositionKey id;

  @Column(name = "QUANTITY", nullable = false, precision = 18, scale = 4)
  private BigDecimal quantity;

  @Column(name = "COST_BASIS", nullable = false, precision = 18, scale = 2)
  private BigDecimal costBasis;

  @Column(name = "MARKET_VALUE", nullable = false, precision = 18, scale = 2)
  private BigDecimal marketValue;

  @Column(name = "CURRENCY_CODE", nullable = false, length = 3)
  private String currencyCode;

  @Column(name = "LAST_MAINT_DATE", nullable = false)
  private LocalDateTime lastMaintDate;

  @Column(name = "LAST_MAINT_USER", nullable = false, length = 8)
  private String lastMaintUser;

  protected InvestmentPosition() {}

  public InvestmentPosition(
      InvestmentPositionKey id,
      BigDecimal quantity,
      BigDecimal costBasis,
      BigDecimal marketValue,
      String currencyCode,
      LocalDateTime lastMaintDate,
      String lastMaintUser) {
    this.id = id;
    this.quantity = quantity;
    this.costBasis = costBasis;
    this.marketValue = marketValue;
    this.currencyCode = currencyCode;
    this.lastMaintDate = lastMaintDate;
    this.lastMaintUser = lastMaintUser;
  }

  public InvestmentPositionKey getId() {
    return id;
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
