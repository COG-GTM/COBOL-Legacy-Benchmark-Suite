package com.portfolio.domain;

import com.portfolio.domain.converter.ClientTypeConverter;
import com.portfolio.domain.converter.PortfolioStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "PORTFOLIO_MASTER")
public class Portfolio {
  @Id
  @Column(name = "PORTFOLIO_ID", length = 8, columnDefinition = "char(8)")
  private String portfolioId;

  @Column(name = "ACCOUNT_NO", length = 10, columnDefinition = "char(10)")
  private String accountNo;

  @Column(name = "CLIENT_NAME", length = 30)
  private String clientName;

  @Convert(converter = ClientTypeConverter.class)
  @Column(name = "CLIENT_TYPE", columnDefinition = "char(1)")
  private ClientType clientType;

  @Column(name = "CREATE_DATE")
  private LocalDate createDate;

  @Column(name = "LAST_MAINT_DATE")
  private LocalDate lastMaintDate;

  @Convert(converter = PortfolioStatusConverter.class)
  @Column(name = "STATUS", columnDefinition = "char(1)")
  private PortfolioStatus status;

  @Column(name = "TOTAL_VALUE", precision = 18, scale = 2)
  private BigDecimal totalValue = BigDecimal.ZERO;

  @Column(name = "CASH_BALANCE", precision = 18, scale = 2)
  private BigDecimal cashBalance = BigDecimal.ZERO;

  @Column(name = "TOTAL_UNITS", precision = 18, scale = 4)
  private BigDecimal totalUnits = BigDecimal.ZERO;

  @Column(name = "TOTAL_COST", precision = 18, scale = 2)
  private BigDecimal totalCost = BigDecimal.ZERO;

  @Column(name = "LAST_USER", length = 8)
  private String lastUser;

  @Column(name = "LAST_TRANS_DATE")
  private LocalDate lastTransDate;

  @Column(name = "ACCOUNT_TYPE", columnDefinition = "char(2)")
  private String accountType;

  @Column(name = "BRANCH_ID", columnDefinition = "char(2)")
  private String branchId;

  @Column(name = "CURRENCY_CODE", columnDefinition = "char(3)")
  private String currencyCode = "USD";

  @Column(name = "RISK_LEVEL", columnDefinition = "char(1)")
  private String riskLevel;

  @Column(name = "CLOSE_DATE")
  private LocalDate closeDate;

  public Portfolio() {}

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

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public ClientType getClientType() {
    return clientType;
  }

  public void setClientType(ClientType clientType) {
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

  public PortfolioStatus getStatus() {
    return status;
  }

  public void setStatus(PortfolioStatus status) {
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

  public BigDecimal getTotalUnits() {
    return totalUnits;
  }

  public void setTotalUnits(BigDecimal totalUnits) {
    this.totalUnits = totalUnits;
  }

  public BigDecimal getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(BigDecimal totalCost) {
    this.totalCost = totalCost;
  }

  public String getLastUser() {
    return lastUser;
  }

  public void setLastUser(String lastUser) {
    this.lastUser = lastUser;
  }

  public LocalDate getLastTransDate() {
    return lastTransDate;
  }

  public void setLastTransDate(LocalDate lastTransDate) {
    this.lastTransDate = lastTransDate;
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

  public LocalDate getCloseDate() {
    return closeDate;
  }

  public void setCloseDate(LocalDate closeDate) {
    this.closeDate = closeDate;
  }
}
