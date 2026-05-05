package com.portfolio.portmstr.model;

import com.portfolio.portmstr.model.enums.ClientType;
import com.portfolio.portmstr.model.enums.PortfolioStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Portfolio Master entity.
 * Mapped from COBOL copybook PORTFLIO.cpy (PORT-RECORD) and
 * DB2 table PORTFOLIO_MASTER (db2-definitions.sql).
 * Represents VSAM KSDS file PORTMSTR with key PORT-ID + PORT-ACCOUNT-NO.
 */
@Entity
@Table(name = "PORTFOLIO_MASTER")
public class PortfolioMaster {

    @Id
    @Column(name = "PORTFOLIO_ID", length = 8, nullable = false)
    @NotBlank
    @Size(max = 8)
    private String portfolioId;

    @Column(name = "ACCOUNT_NO", length = 10)
    @Size(max = 10)
    private String accountNo;

    @Column(name = "CLIENT_NAME", length = 30)
    @Size(max = 30)
    private String clientName;

    @Column(name = "CLIENT_TYPE", length = 1)
    @Enumerated(EnumType.STRING)
    private ClientType clientType;

    @Column(name = "ACCOUNT_TYPE", length = 2)
    @Size(max = 2)
    private String accountType;

    @Column(name = "BRANCH_ID", length = 2)
    @Size(max = 2)
    private String branchId;

    @Column(name = "CLIENT_ID", length = 10)
    @Size(max = 10)
    private String clientId;

    @Column(name = "PORTFOLIO_NAME", length = 50)
    @Size(max = 50)
    private String portfolioName;

    @Column(name = "CURRENCY_CODE", length = 3)
    @Size(max = 3)
    private String currencyCode;

    @Column(name = "RISK_LEVEL", length = 1)
    private Character riskLevel;

    @Column(name = "CREATE_DATE")
    private LocalDate createDate;

    @Column(name = "CLOSE_DATE")
    private LocalDate closeDate;

    @Column(name = "LAST_MAINT_DATE")
    private LocalDate lastMaintDate;

    @Column(name = "STATUS", length = 1, nullable = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private PortfolioStatus status;

    @Column(name = "TOTAL_VALUE", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "CASH_BALANCE", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "LAST_USER", length = 8)
    @Size(max = 8)
    private String lastUser;

    @Column(name = "LAST_TRANS_DATE")
    private LocalDate lastTransDate;

    @Column(name = "LAST_MAINT_TIMESTAMP")
    private LocalDateTime lastMaintTimestamp;

    public PortfolioMaster() {
    }

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

    public Character getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(Character riskLevel) {
        this.riskLevel = riskLevel;
    }

    public LocalDate getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDate createDate) {
        this.createDate = createDate;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
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

    public LocalDateTime getLastMaintTimestamp() {
        return lastMaintTimestamp;
    }

    public void setLastMaintTimestamp(LocalDateTime lastMaintTimestamp) {
        this.lastMaintTimestamp = lastMaintTimestamp;
    }
}
