package com.portfolio.model;

import com.portfolio.model.enums.ClientType;
import com.portfolio.model.enums.PortfolioStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity mapping the PORTFLIO.cpy copybook record structure.
 *
 * Original COBOL layout (src/copybook/common/PORTFLIO.cpy):
 * <pre>
 *  01  PORT-RECORD.
 *      05  PORT-KEY.
 *          10  PORT-ID             PIC X(8).
 *          10  PORT-ACCOUNT-NO     PIC X(10).
 *      05  PORT-CLIENT-INFO.
 *          10  PORT-CLIENT-NAME    PIC X(30).
 *          10  PORT-CLIENT-TYPE    PIC X(1).   -- 88-levels: I, C, T
 *      05  PORT-PORTFOLIO-INFO.
 *          10  PORT-CREATE-DATE    PIC 9(8).
 *          10  PORT-LAST-MAINT     PIC 9(8).
 *          10  PORT-STATUS         PIC X(1).   -- 88-levels: A, C, S
 *      05  PORT-FINANCIAL-INFO.
 *          10  PORT-TOTAL-VALUE    PIC S9(13)V99 COMP-3.
 *          10  PORT-CASH-BALANCE   PIC S9(13)V99 COMP-3.
 *      05  PORT-AUDIT-INFO.
 *          10  PORT-LAST-USER      PIC X(8).
 *          10  PORT-LAST-TRANS     PIC 9(8).
 * </pre>
 *
 * Conversion notes:
 * - COMP-3 (packed decimal) fields -> BigDecimal (always use BigDecimal, set scale explicitly)
 * - PIC 9(8) date fields -> LocalDate
 * - 88-level conditions -> Java enums with JPA AttributeConverter
 */
@Entity
@Table(name = "portfolio")
public class Portfolio {

    @EmbeddedId
    private PortfolioKey key;

    /** PORT-CLIENT-NAME PIC X(30) */
    @Column(name = "client_name", length = 30)
    private String clientName;

    /** PORT-CLIENT-TYPE PIC X(1) with 88-levels: I=Individual, C=Corporate, T=Trust */
    @Column(name = "client_type", length = 1)
    private ClientType clientType;

    /** PORT-CREATE-DATE PIC 9(8) */
    @Column(name = "create_date")
    private LocalDate createDate;

    /** PORT-LAST-MAINT PIC 9(8) */
    @Column(name = "last_maint_date")
    private LocalDate lastMaintDate;

    /** PORT-STATUS PIC X(1) with 88-levels: A=Active, C=Closed, S=Suspended */
    @Column(name = "status", length = 1)
    private PortfolioStatus status;

    /** PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3 */
    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    /** PORT-CASH-BALANCE PIC S9(13)V99 COMP-3 */
    @Column(name = "cash_balance", precision = 15, scale = 2)
    private BigDecimal cashBalance;

    /** PORT-LAST-USER PIC X(8) */
    @Column(name = "last_user", length = 8)
    private String lastUser;

    /** PORT-LAST-TRANS PIC 9(8) */
    @Column(name = "last_trans_date")
    private LocalDate lastTransDate;

    public Portfolio() {
    }

    public PortfolioKey getKey() {
        return key;
    }

    public void setKey(PortfolioKey key) {
        this.key = key;
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

    public LocalDate getLastTransDate() {
        return lastTransDate;
    }

    public void setLastTransDate(LocalDate lastTransDate) {
        this.lastTransDate = lastTransDate;
    }
}
