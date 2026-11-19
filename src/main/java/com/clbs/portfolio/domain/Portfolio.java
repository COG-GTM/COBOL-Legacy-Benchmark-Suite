package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Portfolio master record (VSAM KSDS PORTFILE, copybook PORTFLIO). */
@Entity
@Table(name = "PORTFOLIO")
public class Portfolio {

    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_CLOSED = "C";
    public static final String STATUS_SUSPENDED = "S";

    @EmbeddedId
    private PortfolioKey key;

    @Column(name = "CLIENT_NAME", length = 30, nullable = false)
    private String clientName;

    /** I=Individual, C=Corporate, T=Trust. */
    @Column(name = "CLIENT_TYPE", length = 1)
    private String clientType;

    @Column(name = "CREATE_DATE")
    private LocalDate createDate;

    @Column(name = "LAST_MAINT_DATE")
    private LocalDate lastMaintDate;

    /** A=Active, C=Closed, S=Suspended (PORTUPDT can store any single character). */
    @Column(name = "STATUS", length = 1, nullable = false)
    private String status;

    @Column(name = "TOTAL_VALUE", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue = BigDecimal.ZERO.setScale(2);

    @Column(name = "CASH_BALANCE", precision = 15, scale = 2, nullable = false)
    private BigDecimal cashBalance = BigDecimal.ZERO.setScale(2);

    @Column(name = "LAST_USER", length = 8)
    private String lastUser;

    @Column(name = "LAST_TRANS", length = 8)
    private String lastTrans;

    protected Portfolio() {
    }

    public Portfolio(PortfolioKey key) {
        this.key = key;
    }

    public PortfolioKey getKey() { return key; }
    public String getPortfolioId() { return key.portfolioId(); }
    public String getAccountNo() { return key.accountNo(); }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }
    public LocalDate getCreateDate() { return createDate; }
    public void setCreateDate(LocalDate createDate) { this.createDate = createDate; }
    public LocalDate getLastMaintDate() { return lastMaintDate; }
    public void setLastMaintDate(LocalDate lastMaintDate) { this.lastMaintDate = lastMaintDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public String getLastUser() { return lastUser; }
    public void setLastUser(String lastUser) { this.lastUser = lastUser; }
    public String getLastTrans() { return lastTrans; }
    public void setLastTrans(String lastTrans) { this.lastTrans = lastTrans; }

    @Override
    public String toString() {
        return "Portfolio[" + key.portfolioId() + "/" + key.accountNo() + ", name=" + clientName
                + ", type=" + clientType + ", status=" + status + ", totalValue=" + totalValue
                + ", cashBalance=" + cashBalance + "]";
    }
}
