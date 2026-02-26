package com.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Portfolio Master entity.
 * Migrated from COBOL PORTFOLIO_MASTER DB2 table (db2-definitions.sql)
 * and PORTMSTR VSAM file (vsam-definitions.txt).
 */
@Entity
@Table(name = "PORTFOLIO_MASTER")
public class PortfolioMaster {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "ACCOUNT_TYPE", length = 2, nullable = false)
    private String accountType;

    @Column(name = "BRANCH_ID", length = 2, nullable = false)
    private String branchId;

    @Column(name = "CLIENT_ID", length = 10, nullable = false)
    private String clientId;

    @Column(name = "PORTFOLIO_NAME", length = 50, nullable = false)
    private String portfolioName;

    @Column(name = "CURRENCY_CODE", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "RISK_LEVEL", length = 1, nullable = false)
    private String riskLevel;

    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    @Column(name = "OPEN_DATE", nullable = false)
    private LocalDate openDate;

    @Column(name = "CLOSE_DATE")
    private LocalDate closeDate;

    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    public PortfolioMaster() {}

    public PortfolioMaster(String portfolioId, String accountType, String branchId,
                           String clientId, String portfolioName, String currencyCode,
                           String riskLevel, String status, LocalDate openDate,
                           LocalDateTime lastMaintDate, String lastMaintUser) {
        this.portfolioId = portfolioId;
        this.accountType = accountType;
        this.branchId = branchId;
        this.clientId = clientId;
        this.portfolioName = portfolioName;
        this.currencyCode = currencyCode;
        this.riskLevel = riskLevel;
        this.status = status;
        this.openDate = openDate;
        this.lastMaintDate = lastMaintDate;
        this.lastMaintUser = lastMaintUser;
    }

    // Getters and setters
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
