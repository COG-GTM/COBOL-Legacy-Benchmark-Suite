package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** VSAM TRANFILE as a table (V5); record layout {@code TRNREC.cpy}. */
@Entity
@Table(name = "PORTFOLIO_TRANSACTION")
public class PortfolioTransaction {

  @EmbeddedId private PortfolioTransactionKey id;

  @Column(name = "INVESTMENT_ID", nullable = false, length = 10)
  private String investmentId;

  @Column(name = "TRANSACTION_TYPE", nullable = false, length = 2)
  private String transactionType;

  @Column(name = "QUANTITY", nullable = false, precision = 15, scale = 4)
  private BigDecimal quantity;

  @Column(name = "PRICE", nullable = false, precision = 15, scale = 4)
  private BigDecimal price;

  @Column(name = "AMOUNT", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(name = "CURRENCY_CODE", nullable = false, length = 3)
  private String currencyCode;

  @Column(name = "STATUS", nullable = false, length = 1)
  private String status;

  @Column(name = "PROCESS_TIMESTAMP")
  private LocalDateTime processTimestamp;

  @Column(name = "PROCESS_USER", length = 8)
  private String processUser;

  protected PortfolioTransaction() {}

  public PortfolioTransaction(
      PortfolioTransactionKey id,
      String investmentId,
      String transactionType,
      BigDecimal quantity,
      BigDecimal price,
      BigDecimal amount,
      String currencyCode,
      String status,
      LocalDateTime processTimestamp,
      String processUser) {
    this.id = id;
    this.investmentId = investmentId;
    this.transactionType = transactionType;
    this.quantity = quantity;
    this.price = price;
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.status = status;
    this.processTimestamp = processTimestamp;
    this.processUser = processUser;
  }

  public PortfolioTransactionKey getId() {
    return id;
  }

  public String getInvestmentId() {
    return investmentId;
  }

  public void setInvestmentId(String investmentId) {
    this.investmentId = investmentId;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
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

  public LocalDateTime getProcessTimestamp() {
    return processTimestamp;
  }

  public void setProcessTimestamp(LocalDateTime processTimestamp) {
    this.processTimestamp = processTimestamp;
  }

  public String getProcessUser() {
    return processUser;
  }

  public void setProcessUser(String processUser) {
    this.processUser = processUser;
  }
}
