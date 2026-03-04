package com.cobolbenchmark.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Portfolio Master - migrated from PORTMSTR.cbl / db2-definitions.sql.
 * CRUD operations for portfolio records.
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

    /** PORT-STATUS PIC X(1) - level-88: VALID-STATUS VALUE 'A' 'I' 'C' */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    @Column(name = "OPEN_DATE", nullable = false)
    private LocalDate openDate;

    @Column(name = "CLOSE_DATE")
    private LocalDate closeDate;

    @Column(name = "LAST_MAINT_DATE", nullable = false)
    private java.sql.Timestamp lastMaintDate;

    @Column(name = "LAST_MAINT_USER", length = 8, nullable = false)
    private String lastMaintUser;

    public PortfolioMaster() {
    }

    public PortfolioMaster(PortfolioMaster other) {
        if (other != null) {
            copyFrom(other);
        }
    }

    public void copyFrom(PortfolioMaster other) {
        this.portfolioId = other.portfolioId;
        this.accountType = other.accountType;
        this.branchId = other.branchId;
        this.clientId = other.clientId;
        this.portfolioName = other.portfolioName;
        this.currencyCode = other.currencyCode;
        this.riskLevel = other.riskLevel;
        this.status = other.status;
        this.openDate = other.openDate;
        this.closeDate = other.closeDate;
        this.lastMaintDate = other.lastMaintDate;
        this.lastMaintUser = other.lastMaintUser;
    }

    /**
     * Validate portfolio status - from PORTMSTR.cbl VALID-STATUS VALUE 'A' 'I' 'C'.
     */
    public boolean isValidStatus() {
        return "A".equals(status) || "I".equals(status) || "C".equals(status);
    }

    /**
     * Validate portfolio ID format - from PORTMSTR.cbl / PORTVALD.cbl.
     * Must start with 'PORT' and have numeric suffix.
     */
    public boolean isValidPortfolioId() {
        if (portfolioId == null || portfolioId.length() < 8) {
            return false;
        }
        String prefix = portfolioId.substring(0, 4);
        String suffix = portfolioId.substring(4).trim();
        return "PORT".equals(prefix) && suffix.matches("\\d+");
    }

    // Getters and Setters

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

    public java.sql.Timestamp getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(java.sql.Timestamp lastMaintDate) { this.lastMaintDate = lastMaintDate; }

    public String getLastMaintUser() { return lastMaintUser; }
    public void setLastMaintUser(String lastMaintUser) { this.lastMaintUser = lastMaintUser; }
}
