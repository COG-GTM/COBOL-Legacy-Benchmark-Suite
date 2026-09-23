package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** DB2 {@code PORTFOLIO_MASTER} (V1). */
@Entity
@Table(name = "PORTFOLIO_MASTER")
public class PortfolioMaster {

  @Id
  @Column(name = "PORTFOLIO_ID", nullable = false, length = 8)
  private String portfolioId;

  @Column(name = "ACCOUNT_TYPE", nullable = false, length = 2)
  private String accountType;

  @Column(name = "BRANCH_ID", nullable = false, length = 2)
  private String branchId;

  @Column(name = "CLIENT_ID", nullable = false, length = 10)
  private String clientId;

  @Column(name = "PORTFOLIO_NAME", nullable = false, length = 50)
  private String portfolioName;

  @Column(name = "CURRENCY_CODE", nullable = false, length = 3)
  private String currencyCode;

  @Column(name = "RISK_LEVEL", nullable = false, length = 1)
  private String riskLevel;

  @Column(name = "STATUS", nullable = false, length = 1)
  private String status;

  @Column(name = "OPEN_DATE", nullable = false)
  private LocalDate openDate;

  @Column(name = "CLOSE_DATE")
  private LocalDate closeDate;

  @Column(name = "LAST_MAINT_DATE", nullable = false)
  private LocalDateTime lastMaintDate;

  @Column(name = "LAST_MAINT_USER", nullable = false, length = 8)
  private String lastMaintUser;

  protected PortfolioMaster() {}

  public PortfolioMaster(
      String portfolioId,
      String accountType,
      String branchId,
      String clientId,
      String portfolioName,
      String currencyCode,
      String riskLevel,
      String status,
      LocalDate openDate,
      LocalDate closeDate,
      LocalDateTime lastMaintDate,
      String lastMaintUser) {
    this.portfolioId = portfolioId;
    this.accountType = accountType;
    this.branchId = branchId;
    this.clientId = clientId;
    this.portfolioName = portfolioName;
    this.currencyCode = currencyCode;
    this.riskLevel = riskLevel;
    this.status = status;
    this.openDate = openDate;
    this.closeDate = closeDate;
    this.lastMaintDate = lastMaintDate;
    this.lastMaintUser = lastMaintUser;
  }

  public String getPortfolioId() {
    return portfolioId;
  }

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
  }

  public String getBranchId() {
    return branchId;
  }

  public void setBranchId(String branchId) {
    this.branchId = branchId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getPortfolioName() {
    return portfolioName;
  }

  public void setPortfolioName(String portfolioName) {
    this.portfolioName = portfolioName;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(String riskLevel) {
    this.riskLevel = riskLevel;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getOpenDate() {
    return openDate;
  }

  public void setOpenDate(LocalDate openDate) {
    this.openDate = openDate;
  }

  public LocalDate getCloseDate() {
    return closeDate;
  }

  public void setCloseDate(LocalDate closeDate) {
    this.closeDate = closeDate;
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
