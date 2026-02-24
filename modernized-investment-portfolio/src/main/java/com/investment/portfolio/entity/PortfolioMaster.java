package com.investment.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity mapping for the Portfolio Master table.
 *
 * COBOL Source: PORTFLIO.cpy (PORT-RECORD)
 * DB2 Source: db2-definitions.sql (PORTFOLIO_MASTER)
 *
 * The Portfolio Master holds core portfolio information including client details,
 * status, and financial summary data.
 */
@Entity
@Table(name = "portfolio_master")
public class PortfolioMaster {

    /**
     * Portfolio Identifier.
     * COBOL: PORT-ID PIC X(8)
     * DB2: PORTFOLIO_ID CHAR(8) NOT NULL
     */
    @Id
    @Column(name = "portfolio_id", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String portfolioId;

    /**
     * Account Type code.
     * DB2: ACCOUNT_TYPE CHAR(2) NOT NULL
     */
    @Column(name = "account_type", length = 2, nullable = false)
    @NotNull
    @Size(max = 2)
    private String accountType;

    /**
     * Branch Identifier.
     * DB2: BRANCH_ID CHAR(2) NOT NULL
     */
    @Column(name = "branch_id", length = 2, nullable = false)
    @NotNull
    @Size(max = 2)
    private String branchId;

    /**
     * Client Identifier.
     * COBOL: PORT-ACCOUNT-NO PIC X(10)
     * DB2: CLIENT_ID CHAR(10) NOT NULL
     */
    @Column(name = "client_id", length = 10, nullable = false)
    @NotNull
    @Size(max = 10)
    private String clientId;

    /**
     * Portfolio Name.
     * DB2: PORTFOLIO_NAME VARCHAR(50) NOT NULL
     */
    @Column(name = "portfolio_name", length = 50, nullable = false)
    @NotNull
    @Size(max = 50)
    private String portfolioName;

    /**
     * Currency Code (e.g. USD, EUR, GBP).
     * COBOL: COMMON.cpy CURRENCY-CODES
     * DB2: CURRENCY_CODE CHAR(3) NOT NULL
     */
    @Column(name = "currency_code", length = 3, nullable = false)
    @NotNull
    @Size(max = 3)
    private String currencyCode;

    /**
     * Risk Level indicator.
     * DB2: RISK_LEVEL CHAR(1) NOT NULL
     */
    @Column(name = "risk_level", length = 1, nullable = false)
    @NotNull
    @Size(max = 1)
    private String riskLevel;

    /**
     * Portfolio Status.
     * COBOL: PORT-STATUS PIC X(1) — 'A'=Active, 'C'=Closed, 'S'=Suspended
     * DB2: STATUS CHAR(1) NOT NULL
     */
    @Column(name = "status", length = 1, nullable = false)
    @NotNull
    @Size(max = 1)
    private String status;

    /**
     * Portfolio Open Date.
     * COBOL: PORT-CREATE-DATE PIC 9(8)
     * DB2: OPEN_DATE DATE NOT NULL
     */
    @Column(name = "open_date", nullable = false)
    @NotNull
    private LocalDate openDate;

    /**
     * Portfolio Close Date (nullable for open portfolios).
     * DB2: CLOSE_DATE DATE
     */
    @Column(name = "close_date")
    private LocalDate closeDate;

    /**
     * Last Maintenance Timestamp (audit field).
     * COBOL: PORT-LAST-MAINT PIC 9(8)
     * DB2: LAST_MAINT_DATE TIMESTAMP NOT NULL
     */
    @Column(name = "last_maint_date", nullable = false)
    @NotNull
    private LocalDateTime lastMaintDate;

    /**
     * Last Maintenance User (audit field).
     * COBOL: PORT-LAST-USER PIC X(8)
     * DB2: LAST_MAINT_USER VARCHAR(8) NOT NULL
     */
    @Column(name = "last_maint_user", length = 8, nullable = false)
    @NotNull
    @Size(max = 8)
    private String lastMaintUser;

    /**
     * Investment positions associated with this portfolio.
     */
    @OneToMany(mappedBy = "portfolio")
    private List<InvestmentPosition> positions = new ArrayList<>();

    /**
     * Transactions associated with this portfolio.
     */
    @OneToMany(mappedBy = "portfolio")
    private List<Transaction> transactions = new ArrayList<>();

    public PortfolioMaster() {
    }

    // --- Getters and Setters ---

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

    public List<InvestmentPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<InvestmentPosition> positions) {
        this.positions = positions;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
