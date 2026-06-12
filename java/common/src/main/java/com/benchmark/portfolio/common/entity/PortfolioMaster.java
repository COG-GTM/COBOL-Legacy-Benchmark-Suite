package com.benchmark.portfolio.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Portfolio master record, migrated from PORTFLIO.cpy (PORT-RECORD).
 * VSAM KSDS with RECORD KEY PORT-KEY = PORT-ID + PORT-ACCOUNT-NO,
 * mapped to table PORTFOLIO_MASTER with composite PK (PORTFOLIO_ID, ACCOUNT_NO).
 * PORT-FILLER PIC X(50) is reserved space and is not migrated.
 */
@Entity
@Table(name = "PORTFOLIO_MASTER")
public class PortfolioMaster {

    /** PORT-KEY = PORT-ID PIC X(8) + PORT-ACCOUNT-NO PIC X(10). */
    @EmbeddedId
    private PortfolioMasterId id;

    /** PORT-CLIENT-NAME PIC X(30). */
    @Column(name = "CLIENT_NAME", length = 30, nullable = false)
    private String clientName;

    /** PORT-CLIENT-TYPE PIC X(1); 88-levels: 'I' Individual, 'C' Corporate, 'T' Trust. */
    @Column(name = "CLIENT_TYPE", columnDefinition = "CHAR(1)", length = 1, nullable = false)
    private String clientType;

    /** PORT-CREATE-DATE PIC 9(8) (YYYYMMDD). */
    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDate createDate;

    /** PORT-LAST-MAINT PIC 9(8) (YYYYMMDD); zero value maps to null. */
    @Column(name = "LAST_MAINT_DATE")
    private LocalDate lastMaintDate;

    /** PORT-STATUS PIC X(1); 88-levels: 'A' Active, 'C' Closed, 'S' Suspended. */
    @Column(name = "STATUS", columnDefinition = "CHAR(1)", length = 1, nullable = false)
    private String status;

    /** PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3. */
    @Column(name = "TOTAL_VALUE", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalValue;

    /** PORT-CASH-BALANCE PIC S9(13)V99 COMP-3. */
    @Column(name = "CASH_BALANCE", precision = 15, scale = 2, nullable = false)
    private BigDecimal cashBalance;

    /** PORT-LAST-USER PIC X(8). */
    @Column(name = "LAST_MAINT_USER", length = 8)
    private String lastMaintUser;

    /** PORT-LAST-TRANS PIC 9(8) (unsigned transaction sequence). */
    @Column(name = "LAST_TRANS_NO")
    private Long lastTransNo;

    public PortfolioMasterId getId() {
        return id;
    }

    public void setId(PortfolioMasterId id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getLastMaintUser() {
        return lastMaintUser;
    }

    public void setLastMaintUser(String lastMaintUser) {
        this.lastMaintUser = lastMaintUser;
    }

    public Long getLastTransNo() {
        return lastTransNo;
    }

    public void setLastTransNo(Long lastTransNo) {
        this.lastTransNo = lastTransNo;
    }
}
