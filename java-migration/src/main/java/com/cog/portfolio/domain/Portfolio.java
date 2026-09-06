package com.cog.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** PORTFLIO.cpy PORT-RECORD (VSAM PORTMSTR). */
@Entity
@Table(name = "PORTFOLIO")
public class Portfolio {

    @EmbeddedId
    private PortfolioId id;

    @Column(name = "CLIENT_NAME", length = 30, nullable = false)
    private String clientName;

    @Convert(converter = ClientType.Converter.class)
    @Column(name = "CLIENT_TYPE", length = 1, nullable = false)
    private ClientType clientType;

    @Column(name = "CREATE_DATE")
    private LocalDate createDate;

    @Column(name = "LAST_MAINT")
    private LocalDate lastMaintDate;

    @Convert(converter = PortfolioStatus.Converter.class)
    @Column(name = "STATUS", length = 1, nullable = false)
    private PortfolioStatus status;

    /** PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3 */
    @Column(name = "TOTAL_VALUE", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue = BigDecimal.ZERO.setScale(2);

    /** PORT-CASH-BALANCE PIC S9(13)V99 COMP-3 */
    @Column(name = "CASH_BALANCE", precision = 15, scale = 2, nullable = false)
    private BigDecimal cashBalance = BigDecimal.ZERO.setScale(2);

    @Column(name = "LAST_USER", length = 8)
    private String lastUser;

    @Column(name = "LAST_TRANS")
    private LocalDate lastTransactionDate;

    protected Portfolio() {
    }

    public Portfolio(PortfolioId id, String clientName, ClientType clientType, PortfolioStatus status) {
        this.id = id;
        this.clientName = clientName;
        this.clientType = clientType;
        this.status = status;
    }

    public PortfolioId getId() {
        return id;
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

    public LocalDate getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDate createDate) {
        this.createDate = createDate;
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

    public LocalDate getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(LocalDate lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }
}
