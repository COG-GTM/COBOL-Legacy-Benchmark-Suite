package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** VSAM PORTFILE as a table (V5); record layout {@code PORTFLIO.cpy}. */
@Entity
@Table(name = "PORTFOLIO")
public class Portfolio {

  @EmbeddedId private PortfolioKey id;

  @Column(name = "CLIENT_NAME", nullable = false, length = 30)
  private String clientName;

  @Column(name = "CLIENT_TYPE", nullable = false, length = 1)
  private String clientType;

  @Column(name = "CREATE_DATE")
  private LocalDate createDate;

  @Column(name = "LAST_MAINT_DATE")
  private LocalDate lastMaintDate;

  @Column(name = "STATUS", nullable = false, length = 1)
  private String status;

  @Column(name = "TOTAL_VALUE", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalValue;

  @Column(name = "CASH_BALANCE", nullable = false, precision = 15, scale = 2)
  private BigDecimal cashBalance;

  @Column(name = "LAST_USER", length = 8)
  private String lastUser;

  @Column(name = "LAST_TRANS_DATE")
  private LocalDate lastTransactionDate;

  protected Portfolio() {}

  public Portfolio(
      PortfolioKey id,
      String clientName,
      String clientType,
      LocalDate createDate,
      LocalDate lastMaintDate,
      String status,
      BigDecimal totalValue,
      BigDecimal cashBalance,
      String lastUser,
      LocalDate lastTransactionDate) {
    this.id = id;
    this.clientName = clientName;
    this.clientType = clientType;
    this.createDate = createDate;
    this.lastMaintDate = lastMaintDate;
    this.status = status;
    this.totalValue = totalValue;
    this.cashBalance = cashBalance;
    this.lastUser = lastUser;
    this.lastTransactionDate = lastTransactionDate;
  }

  public PortfolioKey getId() {
    return id;
  }

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public String getClientType() {
    return clientType;
  }

  public void setClientType(String clientType) {
    this.clientType = clientType;
  }

  public LocalDate getCreateDate() {
    return createDate;
  }

  public void setCreateDate(LocalDate createDate) {
    this.createDate = createDate;
  }

  public LocalDate getLastMaintDate() {
    return lastMaintDate;
  }

  public void setLastMaintDate(LocalDate lastMaintDate) {
    this.lastMaintDate = lastMaintDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public BigDecimal getTotalValue() {
    return totalValue;
  }

  public void setTotalValue(BigDecimal totalValue) {
    this.totalValue = totalValue;
  }

  public BigDecimal getCashBalance() {
    return cashBalance;
  }

  public void setCashBalance(BigDecimal cashBalance) {
    this.cashBalance = cashBalance;
  }

  public String getLastUser() {
    return lastUser;
  }

  public void setLastUser(String lastUser) {
    this.lastUser = lastUser;
  }

  public LocalDate getLastTransactionDate() {
    return lastTransactionDate;
  }

  public void setLastTransactionDate(LocalDate lastTransactionDate) {
    this.lastTransactionDate = lastTransactionDate;
  }
}
