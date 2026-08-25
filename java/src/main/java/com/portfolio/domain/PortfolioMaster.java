package com.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity for the DB2 PORTFOLIO_MASTER table
 * ({@code src/database/db2/db2-definitions.sql}).
 */
@Entity
@Table(name = "PORTFOLIO_MASTER")
public class PortfolioMaster {

    /** PORTFOLIO_ID CHAR(8). */
    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    /** ACCOUNT_TYPE CHAR(2). */
    @Column(name = "ACCOUNT_TYPE", length = 2, nullable = false)
    private String accountType;

    /** BRANCH_ID CHAR(2). */
    @Column(name = "BRANCH_ID", length = 2, nullable = false)
    private String branchId;

    /** CLIENT_ID CHAR(10). */
    @Column(name = "CLIENT_ID", length = 10, nullable = false)
    private String clientId;

    /** PORTFOLIO_NAME VARCHAR(50). */
    @Column(name = "PORTFOLIO_NAME", length = 50, nullable = false)
    private String portfolioName;

    /** CURRENCY_CODE CHAR(3). */
    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    /** RISK_LEVEL CHAR(1). */
    @Column(name = "RISK_LEVEL", length = 1, nullable = false)
    private String riskLevel;

    /** STATUS CHAR(1) — A=Active, C=Closed, S=Suspended. */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    /** OPEN_DATE DATE. */
    @Column(name = "OPEN_DATE", nullable = false)
    private LocalDate openDate;

    /** CLOSE_DATE DATE (nullable). */
    @Column(name = "CLOSE_DATE")
    private LocalDate closeDate;

    /** LAST_MAINT_DATE TIMESTAMP. */
    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private LocalDateTime lastMaintDate;

    /** LAST_MAINT_USER VARCHAR(8). */
    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
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
    public LocalDateTime getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDateTime lastMaintDate) { this.lastMaintDate = lastMaintDate; }
    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }
}
