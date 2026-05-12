package com.portfolio.model.entity;

import com.portfolio.model.enums.ClientType;
import com.portfolio.model.enums.PortfolioStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_master")
public class Portfolio {

    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    @Size(max = 8)
    private String portfolioId;

    @Column(name = "account_type", length = 2, nullable = false)
    private String accountType;

    @Column(name = "branch_id", length = 2, nullable = false)
    private String branchId;

    @Column(name = "client_id", length = 10, nullable = false)
    @NotBlank
    private String clientId;

    @Column(name = "portfolio_name", length = 50, nullable = false)
    @NotBlank
    private String portfolioName;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "risk_level", length = 1, nullable = false)
    private String riskLevel;

    @Column(name = "status", length = 1, nullable = false)
    private Character status;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "last_maint_date", nullable = false)
    private LocalDateTime lastMaintDate;

    @Column(name = "last_maint_user", length = 8, nullable = false)
    private String lastMaintUser;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "client_name", length = 30)
    private String clientName;

    @Column(name = "client_type", length = 1)
    private Character clientType;

    public Portfolio() {
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
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

    public Character getStatus() {
        return status;
    }

    public void setStatus(Character status) {
        this.status = status;
    }

    public PortfolioStatus getPortfolioStatus() {
        return status != null ? PortfolioStatus.fromCode(status) : null;
    }

    public void setPortfolioStatus(PortfolioStatus portfolioStatus) {
        this.status = portfolioStatus != null ? portfolioStatus.getCode() : null;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Character getClientTypeCode() {
        return clientType;
    }

    public void setClientTypeCode(Character clientType) {
        this.clientType = clientType;
    }

    public ClientType getClientType() {
        return clientType != null ? ClientType.fromCode(clientType) : null;
    }

    public void setClientType(ClientType type) {
        this.clientType = type != null ? type.getCode() : null;
    }
}
