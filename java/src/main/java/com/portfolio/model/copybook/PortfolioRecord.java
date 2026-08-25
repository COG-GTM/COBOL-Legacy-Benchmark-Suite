package com.portfolio.model.copybook;

import java.math.BigDecimal;

/**
 * Migrated from copybook {@code src/copybook/common/PORTFLIO.cpy} (01 PORT-RECORD).
 *
 * <p>Portfolio master record. Key = PORT-KEY (portfolio id + account no).
 */
public class PortfolioRecord {

    /** PORT-ID PIC X(8). */
    private String id;

    /** PORT-ACCOUNT-NO PIC X(10). */
    private String accountNo;

    /** PORT-CLIENT-NAME PIC X(30). */
    private String clientName;

    /** PORT-CLIENT-TYPE PIC X(1) — I=Individual, C=Corporate, T=Trust (level-88s). */
    private String clientType;

    /** PORT-CREATE-DATE PIC 9(8) — YYYYMMDD. */
    private int createDate;

    /** PORT-LAST-MAINT PIC 9(8) — YYYYMMDD. */
    private int lastMaint;

    /** PORT-STATUS PIC X(1) — A=Active, C=Closed, S=Suspended (level-88s). */
    private String status;

    /** PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3. */
    private BigDecimal totalValue;

    /** PORT-CASH-BALANCE PIC S9(13)V99 COMP-3. */
    private BigDecimal cashBalance;

    /** PORT-LAST-USER PIC X(8). */
    private String lastUser;

    /** PORT-LAST-TRANS PIC 9(8). */
    private int lastTrans;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }
    public int getCreateDate() { return createDate; }
    public void setCreateDate(int createDate) { this.createDate = createDate; }
    public int getLastMaint() { return lastMaint; }
    public void setLastMaint(int lastMaint) { this.lastMaint = lastMaint; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public String getLastUser() { return lastUser; }
    public void setLastUser(String lastUser) { this.lastUser = lastUser; }
    public int getLastTrans() { return lastTrans; }
    public void setLastTrans(int lastTrans) { this.lastTrans = lastTrans; }
}
