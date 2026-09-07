package com.portfolio.domain;

import com.portfolio.domain.converter.TransactionStatusConverter;
import com.portfolio.domain.converter.TransactionTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "TRANSACTION_HISTORY")
public class Transaction {
  @Id
  @Column(name = "TRANSACTION_ID", length = 28)
  private String transactionId;

  @Column(name = "TRANSACTION_DATE")
  private LocalDate transactionDate;

  @Column(name = "TRANSACTION_TIME")
  private LocalTime transactionTime;

  @Column(name = "PORTFOLIO_ID", columnDefinition = "char(8)")
  private String portfolioId;

  @Column(name = "SEQUENCE_NO", columnDefinition = "char(6)")
  private String sequenceNo;

  @Column(name = "INVESTMENT_ID", columnDefinition = "char(10)")
  private String investmentId;

  @Convert(converter = TransactionTypeConverter.class)
  @Column(name = "TRANSACTION_TYPE", columnDefinition = "char(2)")
  private TransactionType transactionType;

  @Column(precision = 18, scale = 4)
  private BigDecimal quantity;

  @Column(precision = 18, scale = 4)
  private BigDecimal price;

  @Column(precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "CURRENCY_CODE", columnDefinition = "char(3)")
  private String currencyCode;

  @Convert(converter = TransactionStatusConverter.class)
  @Column(columnDefinition = "char(1)")
  private TransactionStatus status;

  @Column(name = "PROCESS_DATE")
  private LocalDateTime processDate;

  @Column(name = "PROCESS_USER", length = 8)
  private String processUser;

  public Transaction() {}

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
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

  public String getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(String portfolioId) {
    this.portfolioId = portfolioId;
  }

  public String getSequenceNo() {
    return sequenceNo;
  }

  public void setSequenceNo(String sequenceNo) {
    this.sequenceNo = sequenceNo;
  }

  public String getInvestmentId() {
    return investmentId;
  }

  public void setInvestmentId(String investmentId) {
    this.investmentId = investmentId;
  }

  public TransactionType getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(TransactionType transactionType) {
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

  public TransactionStatus getStatus() {
    return status;
  }

  public void setStatus(TransactionStatus status) {
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
