package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** VSAM position master as a table (V5); record layout {@code POSREC.cpy}. */
@Entity
@Table(name = "POSITION_MASTER")
public class PositionMaster {

  @EmbeddedId private PositionMasterKey id;

  @Column(name = "QUANTITY", nullable = false, precision = 15, scale = 4)
  private BigDecimal quantity;

  @Column(name = "COST_BASIS", nullable = false, precision = 15, scale = 2)
  private BigDecimal costBasis;

  @Column(name = "MARKET_VALUE", nullable = false, precision = 15, scale = 2)
  private BigDecimal marketValue;

  @Column(name = "CURRENCY_CODE", nullable = false, length = 3)
  private String currencyCode;

  @Column(name = "STATUS", nullable = false, length = 1)
  private String status;

  @Column(name = "LAST_MAINT_TIMESTAMP")
  private LocalDateTime lastMaintTimestamp;

  @Column(name = "LAST_MAINT_USER", length = 8)
  private String lastMaintUser;

  protected PositionMaster() {}

  public PositionMaster(
      PositionMasterKey id,
      BigDecimal quantity,
      BigDecimal costBasis,
      BigDecimal marketValue,
      String currencyCode,
      String status,
      LocalDateTime lastMaintTimestamp,
      String lastMaintUser) {
    this.id = id;
    this.quantity = quantity;
    this.costBasis = costBasis;
    this.marketValue = marketValue;
    this.currencyCode = currencyCode;
    this.status = status;
    this.lastMaintTimestamp = lastMaintTimestamp;
    this.lastMaintUser = lastMaintUser;
  }

  public PositionMasterKey getId() {
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getLastMaintTimestamp() {
    return lastMaintTimestamp;
  }

  public void setLastMaintTimestamp(LocalDateTime lastMaintTimestamp) {
    this.lastMaintTimestamp = lastMaintTimestamp;
  }

  public String getLastMaintUser() {
    return lastMaintUser;
  }

  public void setLastMaintUser(String lastMaintUser) {
    this.lastMaintUser = lastMaintUser;
  }
}
