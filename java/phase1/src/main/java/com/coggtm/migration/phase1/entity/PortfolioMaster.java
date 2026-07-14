package com.coggtm.migration.phase1.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_master")
public class PortfolioMaster {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    private String portfolioId;

    @Column(name = "account_type", length = 2, nullable = false)
    private String accountType;

    @Column(name = "branch_id", length = 2, nullable = false)
    private String branchId;

    @Column(name = "client_id", length = 10, nullable = false)
    private String clientId;

    @Column(name = "portfolio_name", length = 50, nullable = false)
    private String portfolioName;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "risk_level", length = 1, nullable = false)
    private String riskLevel;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "last_maint_date", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8, nullable = false)
    private String lastMaintUser;

    public PortfolioMaster() {
    }

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
