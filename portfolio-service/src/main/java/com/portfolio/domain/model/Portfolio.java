package com.portfolio.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aggregate root — maps COBOL PORTFLIO.cpy PORT-RECORD.
 * <p>
 * Child sessions will add {@code applyBuy()}, {@code applySell()}, etc.
 */
@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @Column(name = "port_id", length = 8)
    private String portfolioId;

    @Column(name = "account_no", length = 10, nullable = false)
    private String accountNumber;

    @Column(name = "client_name", length = 30)
    private String clientName;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 12)
    private ClientType clientType;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "last_maintenance")
    private LocalDate lastMaintenance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private PortfolioStatus status;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "cash_balance", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "last_user", length = 8)
    private String lastUser;

    @Column(name = "last_trans_date")
    private LocalDate lastTransDate;

    protected Portfolio() { /* JPA */ }

    public Portfolio(String portfolioId, String accountNumber, String clientName,
                     ClientType clientType) {
        this.portfolioId = portfolioId;
        this.accountNumber = accountNumber;
        this.clientName = clientName;
        this.clientType = clientType;
        this.status = PortfolioStatus.ACTIVE;
        this.totalValue = BigDecimal.ZERO;
        this.cashBalance = BigDecimal.ZERO;
        this.createDate = LocalDate.now();
        this.lastMaintenance = LocalDate.now();
    }

    // --- Getters ---

    public String getPortfolioId() { return portfolioId; }
    public String getAccountNumber() { return accountNumber; }
    public String getClientName() { return clientName; }
    public ClientType getClientType() { return clientType; }
    public LocalDate getCreateDate() { return createDate; }
    public LocalDate getLastMaintenance() { return lastMaintenance; }
    public PortfolioStatus getStatus() { return status; }
    public BigDecimal getTotalValue() { return totalValue; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public String getLastUser() { return lastUser; }
    public LocalDate getLastTransDate() { return lastTransDate; }

    // --- Mutators (to be expanded by child sessions) ---

    public void setStatus(PortfolioStatus status) { this.status = status; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setClientType(ClientType clientType) { this.clientType = clientType; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public void markMaintenance(String userId) {
        this.lastMaintenance = LocalDate.now();
        this.lastUser = userId;
        this.lastTransDate = LocalDate.now();
    }
}
