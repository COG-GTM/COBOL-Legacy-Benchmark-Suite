package com.portfolio.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public class TransactionDto {
  @Pattern(regexp = "PORT\\d{4}")
  private String portfolioId;

  @Pattern(regexp = "\\d{10}")
  private String accountNo;

  @Pattern(regexp = "STK|BND|MMF|ETF")
  private String investmentType;

  private String investmentId;
  private String transactionType;

  @DecimalMin("0")
  private BigDecimal quantity;

  @DecimalMin("0")
  @Digits(integer = 14, fraction = 4)
  private BigDecimal price;

  @DecimalMin("0")
  @DecimalMax("9999999999999.99")
  private BigDecimal amount;

  private String currencyCode;
  private LocalDate transactionDate;
  private LocalTime transactionTime;
  private String processUser;

  public String getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(String portfolioId) {
    this.portfolioId = portfolioId;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public void setAccountNo(String accountNo) {
    this.accountNo = accountNo;
  }

  public String getInvestmentType() {
    return investmentType;
  }

  public void setInvestmentType(String investmentType) {
    this.investmentType = investmentType;
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

  public String getProcessUser() {
    return processUser;
  }

  public void setProcessUser(String processUser) {
    this.processUser = processUser;
  }
}
