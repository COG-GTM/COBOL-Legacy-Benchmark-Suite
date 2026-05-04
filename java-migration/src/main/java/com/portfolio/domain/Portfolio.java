package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Portfolio Master entity - migrated from COBOL PORTFLIO.cpy.
 *
 * COBOL field mappings:
 * - PORT-ID (PIC X(8)) -> portfolioId (String, length=8)
 * - PORT-ACCOUNT-NO (PIC X(10)) -> accountNo (String, length=10)
 * - PORT-CLIENT-NAME (PIC X(30)) -> clientName (String, length=30)
 * - PORT-CLIENT-TYPE (PIC X(1)) -> clientType (String, length=1) [I/C/T]
 * - PORT-TOTAL-VALUE (PIC S9(13)V99 COMP-3) -> totalValue (BigDecimal, precision=15, scale=2)
 * - PORT-CASH-BALANCE (PIC S9(13)V99 COMP-3) -> cashBalance (BigDecimal, precision=15, scale=2)
 */
@Entity
@Table(name = "portfolio_master")
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "account_type", length = 2, nullable = false)
    private String accountType;

    @Column(name = "branch_id", length = 2, nullable = false)
    private String branchId;

    @Column(name = "client_id", length = 10, nullable = false)
    private String clientId;

    @Column(name = "client_name", length = 30, nullable = false)
    private String clientName;

    @Column(name = "client_type", length = 1, nullable = false)
    private String clientType;

    @Column(name = "portfolio_name", length = 50)
    private String portfolioName;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "risk_level", length = 1)
    private String riskLevel;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "total_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal cashBalance;

    @Column(name = "last_maint_date", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8, nullable = false)
    private String lastMaintUser;

    @Column(name = "last_trans_date")
    private LocalDate lastTransDate;

    @Column(name = "filler", length = 50)
    private String filler;

    public Portfolio() {
        this.totalValue = BigDecimal.ZERO;
        this.cashBalance = BigDecimal.ZERO;
        this.currencyCode = "USD";
        this.status = "A";
        this.lastMaintDate = LocalDateTime.now();
    }

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }
    public String getPortfolioName() { return portfolioName; }
    public void setPortfolioName(String portfolioName) { this.portfolioName = portfolioName; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getOpenDate() { return openDate; }
    public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDateTime lastMaintDate) { this.lastMaintDate = lastMaintDate; }
    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }
    public LocalDate getLastTransDate() { return lastTransDate; }
    public void setLastTransDate(LocalDate lastTransDate) { this.lastTransDate = lastTransDate; }
    public String getFiller() { return filler; }
    public void setFiller(String filler) { this.filler = filler; }

    public boolean isActive() { return "A".equals(status); }
    public boolean isClosed() { return "C".equals(status); }
    public boolean isSuspended() { return "S".equals(status); }
}
