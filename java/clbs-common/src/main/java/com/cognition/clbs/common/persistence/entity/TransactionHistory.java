package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** DB2 {@code TRANSACTION_HISTORY} (V1). */
@Entity
@Table(name = "TRANSACTION_HISTORY")
public class TransactionHistory {

  @Id
  @Column(name = "TRANSACTION_ID", nullable = false, length = 20)
  private String transactionId;

  @Column(name = "PORTFOLIO_ID", nullable = false, length = 8)
  private String portfolioId;

  @Column(name = "TRANSACTION_DATE", nullable = false)
  private LocalDate transactionDate;

  @Column(name = "TRANSACTION_TIME", nullable = false)
  private LocalTime transactionTime;

  @Column(name = "INVESTMENT_ID", nullable = false, length = 10)
  private String investmentId;

  @Column(name = "TRANSACTION_TYPE", nullable = false, length = 2)
  private String transactionType;

  @Column(name = "QUANTITY", nullable = false, precision = 18, scale = 4)
  private BigDecimal quantity;

  @Column(name = "PRICE", nullable = false, precision = 18, scale = 4)
  private BigDecimal price;

  @Column(name = "AMOUNT", nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "CURRENCY_CODE", nullable = false, length = 3)
  private String currencyCode;

  @Column(name = "STATUS", nullable = false, length = 1)
  private String status;

  @Column(name = "PROCESS_DATE", nullable = false)
  private LocalDateTime processDate;

  @Column(name = "PROCESS_USER", nullable = false, length = 8)
  private String processUser;

  protected TransactionHistory() {}

  public TransactionHistory(
      String transactionId,
      String portfolioId,
      LocalDate transactionDate,
      LocalTime transactionTime,
      String investmentId,
      String transactionType,
      BigDecimal quantity,
      BigDecimal price,
      BigDecimal amount,
      String currencyCode,
      String status,
      LocalDateTime processDate,
      String processUser) {
    this.transactionId = transactionId;
    this.portfolioId = portfolioId;
    this.transactionDate = transactionDate;
    this.transactionTime = transactionTime;
    this.investmentId = investmentId;
    this.transactionType = transactionType;
    this.quantity = quantity;
    this.price = price;
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.status = status;
    this.processDate = processDate;
    this.processUser = processUser;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public String getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(String portfolioId) {
    this.portfolioId = portfolioId;
  }

  public LocalDate getTransactionDate() {
    return transactionDate;
  }

  public void setTransactionDate(LocalDate transactionDate) {
    this.transactionDate = transactionDate;
  }

  public LocalTime getTransactionTime() {
    return transactionTime;
  }

  public void setTransactionTime(LocalTime transactionTime) {
    this.transactionTime = transactionTime;
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

  public LocalDateTime getProcessDate() {
    return processDate;
  }

  public void setProcessDate(LocalDateTime processDate) {
    this.processDate = processDate;
  }

  public String getProcessUser() {
    return processUser;
  }

  public void setProcessUser(String processUser) {
    this.processUser = processUser;
  }
}
